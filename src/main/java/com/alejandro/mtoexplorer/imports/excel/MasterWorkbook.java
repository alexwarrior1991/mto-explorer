package com.alejandro.mtoexplorer.imports.excel;

import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Un maestro abierto en streaming: las filas se leen de una en una sin cargar el libro en memoria.
 * En streaming cada hoja se recorre una sola vez, así que cada forEachRow abre su propio lector.
 */
public final class MasterWorkbook {

    private final Path file;

    public MasterWorkbook(Path file) {
        this.file = file;
    }

    /**
     * Recorre las filas con datos de la hoja; la primera fila es la cabecera. Devuelve cuántas ha leído.
     */
    public int forEachRow(String sheetName, Consumer<SheetRow> action) {
        return forEachRow(sheetName, true, action);
    }

    /**
     * Como forEachRow, pero si la hoja no está devuelve 0 en vez de fallar (hojas opcionales del maestro).
     */
    public int forEachRowIfPresent(String sheetName, Consumer<SheetRow> action) {
        return forEachRow(sheetName, false, action);
    }

    private int forEachRow(String sheetName, boolean required, Consumer<SheetRow> action) {
        try (Workbook workbook = StreamingReader.builder().rowCacheSize(200).bufferSize(8192).open(file.toFile())) {
            // excel-streaming-reader no devuelve null si falta la hoja: lanza MissingSheetException.
            // Por eso se pregunta antes por su índice.
            if (workbook.getSheetIndex(sheetName) < 0) {
                if (!required) {
                    return 0;
                }
                throw new MasterFormatException(file.getFileName() + " no tiene la hoja " + sheetName);
            }
            Sheet sheet = workbook.getSheet(sheetName);
            Iterator<Row> rows = sheet.rowIterator();
            if (!rows.hasNext()) {
                return 0;
            }
            Map<String, Integer> header = header(rows.next());
            int count = 0;
            while (rows.hasNext()) {
                SheetRow row = new SheetRow(rows.next(), header);
                if (row.isEmpty()) {
                    continue;
                }
                action.accept(row);
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede leer " + file, e);
        }
    }

    private static Map<String, Integer> header(Row row) {
        Map<String, Integer> header = new HashMap<>();
        for (Cell cell : row) {
            String name = cell.getStringCellValue();
            if (name != null && !name.isBlank()) {
                header.put(name.trim(), cell.getColumnIndex());
            }
        }
        return header;
    }
}
