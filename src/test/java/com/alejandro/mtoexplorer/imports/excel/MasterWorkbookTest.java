package com.alejandro.mtoexplorer.imports.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La lectura de Excel, sin Quarkus: JUnit a secas sobre un libro de una hoja escrito en el test. */
class MasterWorkbookTest {

    @TempDir
    Path dir;

    private MasterWorkbook workbook;

    @BeforeEach
    void writeWorkbook() throws IOException {
        Path file = dir.resolve("libro.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            Sheet sheet = wb.createSheet("DATOS");
            // Las columnas en un orden cualquiera: se leen por nombre.
            row(sheet, 0, "KP", "NOMBRE", "ACTIVO", "SIN_DATO");
            row(sheet, 1, "12345.5", "uno", "SI", null);
            row(sheet, 2, "-77.96", "dos", "NO", "SI");
            // Fila vacía en medio: se salta.
            sheet.createRow(3);
            row(sheet, 4, " 1,5 ", "tres", null, "NO");
            wb.write(out);
        }
        workbook = new MasterWorkbook(file);
    }

    @Test
    void readsRowsByColumnNameAndSkipsEmptyOnes() {
        List<String> names = new ArrayList<>();
        List<Double> kps = new ArrayList<>();
        int count = workbook.forEachRow("DATOS", r -> {
            names.add(r.string("NOMBRE"));
            kps.add(r.decimal("KP"));
        });
        assertEquals(3, count);
        assertEquals(List.of("uno", "dos", "tres"), names);
        // Texto con coma decimal y espacios también es un número.
        assertEquals(List.of(12345.5, -77.96, 1.5), kps);
    }

    @Test
    void yesNoAndOptionalYesNo() {
        List<Boolean> active = new ArrayList<>();
        List<Boolean> optional = new ArrayList<>();
        workbook.forEachRow("DATOS", r -> {
            active.add(r.yesNo("ACTIVO"));
            optional.add(r.optionalYesNo("SIN_DATO"));
        });
        // Vacío en yesNo es false; en optionalYesNo es null ("sin dato").
        assertEquals(List.of(true, false, false), active);
        assertNull(optional.get(0));
        assertEquals(true, optional.get(1));
        assertEquals(false, optional.get(2));
    }

    @Test
    void missingRequiredSheetFailsButOptionalOneReadsNothing() {
        MasterFormatException e = assertThrows(MasterFormatException.class,
                () -> workbook.forEachRow("NO_EXISTE", r -> { }));
        assertTrue(e.getMessage().contains("NO_EXISTE"));
        assertEquals(0, workbook.forEachRowIfPresent("NO_EXISTE", r -> { }));
    }

    @Test
    void missingColumnSaysWhichAndWhere() {
        MasterFormatException e = assertThrows(MasterFormatException.class,
                () -> workbook.forEachRow("DATOS", r -> r.string("COLUMNA_QUE_NO_ESTA")));
        assertEquals("Columna COLUMNA_QUE_NO_ESTA, fila 2: la columna no existe en la hoja", e.getMessage());
    }

    @Test
    void textWhereANumberIsExpectedFails() {
        MasterFormatException e = assertThrows(MasterFormatException.class,
                () -> workbook.forEachRow("DATOS", r -> r.decimal("NOMBRE")));
        assertTrue(e.getMessage().contains("se esperaba un número"));
    }

    private static void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int c = 0; c < values.length; c++) {
            if (values[c] != null) {
                row.createCell(c).setCellValue(values[c]);
            }
        }
    }
}
