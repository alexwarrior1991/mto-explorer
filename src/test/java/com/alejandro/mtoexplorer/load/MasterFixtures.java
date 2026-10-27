package com.alejandro.mtoexplorer.load;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Escribe dos maestros pequeños e inventados con la misma forma que los reales. Los tests no
 * dependen de los datos de ingeniería, que no están en el repositorio.
 *
 * <p>Viene hecho porque escribir Excel con POI no es lo que se aprende aquí. Úsalo desde un
 * {@code QuarkusTestResourceLifecycleManager} que cree una carpeta temporal, llame a
 * {@link #writeAll(Path)} y devuelva {@code mto.data-dir} apuntando a ella (docs/fase-1.md, paso 8).
 *
 * <p>Qué contiene, para escribir los asserts: EP1 (2 estaciones, vías TRACK 1 y TRACK 2) y EP2;
 * 6 perfiles (5 en EP1 y 1 en EP2; uno de cada paquete marcado REVISAR); en EP1/TRACK 1 los perfiles de orden 2 y 3 comparten
 * PROFILE_ID 1-1.02, como pasa en el maestro real; 3 ménsulas (dos del perfil de orden 2);
 * 3 LOVs (2 de Sectioning, una de ellas REVISAR, y HEB-200 de PoleType); y 3 valores en
 * NO_RECONOCIDO (uno en el maestro de LOVs y dos en el de perfiles).
 */
public final class MasterFixtures {

    private MasterFixtures() {
    }

    public static void writeAll(Path dir) {
        writeLovMaster(dir.resolve("lov-master.xlsx"));
        writeProfileMaster(dir.resolve("profile-master.xlsx"));
    }

    public static void writeLovMaster(Path file) {
        write(file, wb -> {
            sheet(wb, "LOVS",
                    new Object[]{"ENTIDAD", "CODIGO", "DESCRIPCION_EN", "DESCRIPCION_ES", "TIPO", "N_PLANO", "ENABLED",
                            "ORIGEN", "CATEGORIAS_BOQ", "EPS", "USOS_EN_TRACKS", "REVISAR"},
                    new Object[]{"Sectioning", "A/S", "Overlap Anchorage", null, null, null, "SI", "LEGEND+TRACK", null,
                            "EP1, EP2", 12, "NO"},
                    new Object[]{"Sectioning", "S/A", "Anchorage Overlap", "Anclaje de seccionamiento", null, null, "SI",
                            "TRACK", null, "EP1", 4, "SI"},
                    new Object[]{"PoleType", "HEB-200", "Pole HEB 200", "Poste HEB 200", null, 1234, "SI", "LEGEND",
                            "Poles", "EP1", 30, "NO"});
            sheet(wb, "NO_RECONOCIDO",
                    new Object[]{"TIPO", "VALOR", "EP", "HOJA", "PRIMERA_FILA", "APARICIONES"},
                    new Object[]{"codigo sin Sectioning habilitado", "AnXX", "EP1", "HR Track 1", 40, 2});
        });
    }

    public static void writeProfileMaster(Path file) {
        write(file, wb -> {
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

            Sheet eps = sheet(wb, "EPS",
                    new Object[]{"EP", "NOMBRE", "INITIAL_PACKAGE", "LENGTH", "START_DATE", "END_DATE",
                            "COMPANY_ID_NUMBER", "ENABLED"},
                    new Object[]{"EP1", "Paquete uno", "SI", 12000, LocalDate.of(2020, 1, 15), LocalDate.of(2022, 6, 30),
                            "B00000000", "SI"},
                    new Object[]{"EP2", "Paquete dos", "NO", 8000, null, null, "B00000000", "SI"});
            eps.getRow(1).getCell(4).setCellStyle(date);
            eps.getRow(1).getCell(5).setCellStyle(date);

            sheet(wb, "STATIONS",
                    new Object[]{"EP", "NOMBRE"},
                    new Object[]{"EP1", "AAA"},
                    new Object[]{"EP1", "BBB"},
                    new Object[]{"EP2", "CCC"});
            sheet(wb, "TRACKS",
                    new Object[]{"EP", "NOMBRE", "ESTACIONES", "ENABLED", "HOJA_ORIGEN", "FILA_INICIO", "FILA_FIN"},
                    new Object[]{"EP1", "TRACK 1", "AAA | BBB", "SI", "HR Track 1", null, null},
                    new Object[]{"EP1", "TRACK 2", "AAA", "SI", "HR Track 2", null, null},
                    new Object[]{"EP2", "TRACK 1", null, "SI", "HR Track 1", null, null});
            sheet(wb, "PROFILES",
                    new Object[]{"EP", "VIA", "PROFILE_ID", "KP", "ORDEN", "PROFILE_STATUS", "SECTIONING", "ANCHORAGE",
                            "ANCHORAGE_FOUNDATION", "FOUNDATION", "POLE_TYPE", "PORTAL", "RETURN_SUPPORT",
                            "SECTIONING_FEEDING", "SUPPORT_TYPE", "ASSEMBLY_CONFIGURATION", "SPAN",
                            "HEIGHT_CANTILEVER_SUPPORT", "POLE_GAUGE_LOCATION", "RAIL_POLE_DISTANCE", "ENABLED",
                            "REVISAR", "HOJA_ORIGEN", "FILA_ORIGEN"},
                    profile("EP1", "TRACK 1", "1-1.01", 12000, 1, "A/S", "HEB-200", "NO", 6),
                    profile("EP1", "TRACK 1", "1-1.02", 12054.5, 2, null, "HEB-200", "SI", 8),
                    // PROFILE_ID repetido en la misma vía, como pasa en el maestro real.
                    profile("EP1", "TRACK 1", "1-1.02", 12110, 3, "S/A", null, "NO", 10),
                    profile("EP1", "TRACK 1", "1-1.04", 13600, 4, null, "HEB-200", "NO", 12),
                    profile("EP1", "TRACK 2", "1-2.01", 12020, 1, null, "HEB-200", "NO", 6),
                    profile("EP2", "TRACK 1", "2-1.01", 500, 1, null, "HEB-200", "SI", 6));
            sheet(wb, "CANTILEVERS",
                    new Object[]{"EP", "VIA", "PROFILE_ID", "ORDEN", "SLOT", "CANTILEVER_TYPE", "STAGGER",
                            "CATENARY_HEIGHT", "CW_ELEVATION", "CW_HEIGHT", "WIND_DEFLECTION", "ARM_ANGLE",
                            "STEADY_ARM_TYPE", "STEADY_ARM_LENGTH", "ENABLED", "REVISAR", "FILA_ORIGEN"},
                    new Object[]{"EP1", "TRACK 1", "1-1.02", 2, 1, "EMT-TT", -70, 1.1, 0.45, 5.25, null, 12.5, "SA-1",
                            1100, "SI", "NO", 8},
                    new Object[]{"EP1", "TRACK 1", "1-1.02", 2, 2, "EMT-1", -20, 0.9, null, null, null, null, null,
                            null, "SI", "SI", 8},
                    new Object[]{"EP1", "TRACK 1", "1-1.01", 1, 1, "EMT-2T", 35, 1.4, null, 5.33, null, null, null,
                            null, "SI", "NO", 6});
            sheet(wb, "NO_RECONOCIDO",
                    new Object[]{"TIPO", "VALOR", "EP", "HOJA", "PRIMERA_FILA", "APARICIONES"},
                    new Object[]{"codigo sin Sectioning habilitado", "AnMP T", "EP1", "HR Track 1", 283, 1},
                    new Object[]{"codigo sin Sectioning habilitado", "AnRW", "EP2", "HR Track 1", 32, 3});
            // Hoja que el explorador no lee: tiene que dar igual que esté.
            sheet(wb, "NO_MAPEADO", new Object[]{"EP", "VIA"}, new Object[]{"EP1", "TRACK 1"});
        });
    }

    private static Object[] profile(String ep, String track, String id, double kp, int order, String sectioning,
                                    String poleType, String toReview, int sourceRow) {
        return new Object[]{ep, track, id, kp, order, "DEFINITIVE", sectioning, null, null, "C4R", poleType, null,
                null, null, null, null, 54.5, null, null, null, "SI", toReview, "HR " + track, sourceRow};
    }

    private static Sheet sheet(XSSFWorkbook wb, String name, Object[]... rows) {
        Sheet sheet = wb.createSheet(name);
        for (int r = 0; r < rows.length; r++) {
            Row row = sheet.createRow(r);
            for (int c = 0; c < rows[r].length; c++) {
                Object value = rows[r][c];
                switch (value) {
                    case null -> { }
                    case String s -> row.createCell(c).setCellValue(s);
                    case Number n -> row.createCell(c).setCellValue(n.doubleValue());
                    case LocalDate d -> row.createCell(c).setCellValue(
                            Date.from(d.atStartOfDay().toInstant(ZoneOffset.UTC)));
                    default -> throw new IllegalArgumentException("Tipo no soportado: " + value.getClass());
                }
            }
        }
        return sheet;
    }

    private interface WorkbookWriter {
        void write(XSSFWorkbook wb);
    }

    private static void write(Path file, WorkbookWriter writer) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
            writer.write(wb);
            wb.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
