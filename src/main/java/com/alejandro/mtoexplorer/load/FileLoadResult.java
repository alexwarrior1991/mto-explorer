package com.alejandro.mtoexplorer.load;

/**
 * Lo que pasó con un fichero en una carga.
 */
public record FileLoadResult(String file, Status status, int rows, long durationMs, String detail) {

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

    static FileLoadResult loaded(String file, int rows, long durationMs) {
        return new FileLoadResult(file, Status.LOADED, rows, durationMs, null);
    }

    static FileLoadResult unchanged(String file) {
        return new FileLoadResult(file, Status.UNCHANGED, 0, 0, null);
    }

    static FileLoadResult missing(String file, String detail) {
        return new FileLoadResult(file, Status.MISSING, 0, 0, detail);
    }
}
