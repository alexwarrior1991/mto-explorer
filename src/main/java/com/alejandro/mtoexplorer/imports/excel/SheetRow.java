package com.alejandro.mtoexplorer.imports.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalDate;
import java.util.Map;

/**
 * Una fila de una hoja, leída por nombre de columna (el generador puede cambiar el orden).
 */
public final class SheetRow {

    private final Row row;
    private final Map<String, Integer> header;

    SheetRow(Row row, Map<String, Integer> header) {
        this.row = row;
        this.header = header;
    }

    public int rowNumber() {
        return row.getRowNum() + 1;
    }

    public String string(String column) {
        Cell cell = cell(column);
        if (cell == null) {
            return null;
        }
        return switch (type(cell)) {
            case STRING -> blankToNull(cell.getStringCellValue());
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            default -> null;
        };
    }

    public Double decimal(String column) {
        Cell cell = cell(column);
        if (cell == null) {
            return null;
        }
        return switch (type(cell)) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                String s = blankToNull(cell.getStringCellValue());
                try {
                    yield s == null ? null : Double.valueOf(s.replace(',', '.'));
                } catch (NumberFormatException e) {
                    throw new MasterFormatException(column, rowNumber(), "se esperaba un número y hay '" + s + "'");
                }
            }
            default -> null;
        };
    }

    public Integer integer(String column) {
        Double d = decimal(column);
        return d == null ? null : (int) Math.round(d);
    }

    /**
     * SI/NO de los maestros. Vacío cuenta como false.
     */
    public boolean yesNo(String column) {
        String s = string(column);
        return s != null && (s.equalsIgnoreCase("SI") || s.equalsIgnoreCase("SÍ") || s.equalsIgnoreCase("true"));
    }

    /**
     * SI/NO que admite "sin dato": vacío es null.
     */
    public Boolean optionalYesNo(String column) {
        String s = string(column);
        return s == null ? null : yesNo(column);
    }

    public LocalDate date(String column) {
        Cell cell = cell(column);
        if (cell == null || type(cell) != CellType.NUMERIC) {
            return null;
        }
        return cell.getLocalDateTimeCellValue().toLocalDate();
    }

    /**
     * Fila sin nada en la primera columna: las hojas suelen acabar con filas vacías con formato.
     */
    boolean isEmpty() {
        Cell first = row.getCell(0);
        return first == null || type(first) == CellType.BLANK
                || (type(first) == CellType.STRING && first.getStringCellValue().isBlank());
    }

    private Cell cell(String column) {
        Integer index = header.get(column);
        if (index == null) {
            throw new MasterFormatException(column, rowNumber(), "la columna no existe en la hoja");
        }
        return row.getCell(index);
    }

    private static CellType type(Cell cell) {
        return cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
