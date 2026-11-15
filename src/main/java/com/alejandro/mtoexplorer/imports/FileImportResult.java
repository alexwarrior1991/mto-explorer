package com.alejandro.mtoexplorer.imports;

/**
 * Lo que pasó con un fichero en una carga.
 */
public record FileImportResult(String file, Status status, int rows, long durationMs, String detail) {

    public enum Status {
        /**
         * Se ha cargado.
         */
        LOADED,
        /**
         * No ha cambiado desde la última carga; no se ha tocado.
         */
        UNCHANGED,
        /**
         * No está en la carpeta de datos.
         */
        MISSING
    }

    static FileImportResult loaded(String file, int rows, long durationMs) {
        return new FileImportResult(file, Status.LOADED, rows, durationMs, null);
    }

    static FileImportResult unchanged(String file) {
        return new FileImportResult(file, Status.UNCHANGED, 0, 0, null);
    }

    static FileImportResult missing(String file, String detail) {
        return new FileImportResult(file, Status.MISSING, 0, 0, detail);
    }
}
