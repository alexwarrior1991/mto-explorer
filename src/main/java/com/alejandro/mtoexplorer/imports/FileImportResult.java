package com.alejandro.mtoexplorer.imports;

/**
 * Lo que pasó con un fichero en una importación.
 *
 * @param master LOV o PROFILE: qué maestro es (decide qué cachés hay que vaciar)
 * @param file   el nombre del fichero
 */
public record FileImportResult(String master, String file, Status status, int rows, long durationMs, String detail) {

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

    public boolean loaded() {
        return status == Status.LOADED;
    }

    static FileImportResult loaded(String master, String file, int rows, long durationMs) {
        return new FileImportResult(master, file, Status.LOADED, rows, durationMs, null);
    }

    static FileImportResult unchanged(String master, String file) {
        return new FileImportResult(master, file, Status.UNCHANGED, 0, 0, null);
    }

    static FileImportResult missing(String master, String file, String detail) {
        return new FileImportResult(master, file, Status.MISSING, 0, 0, detail);
    }
}
