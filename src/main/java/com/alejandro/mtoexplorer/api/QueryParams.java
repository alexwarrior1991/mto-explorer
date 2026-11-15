package com.alejandro.mtoexplorer.api;

/** Ayudas para leer parámetros de consulta. */
final class QueryParams {

    private QueryParams() {
    }

    /** "?track=" (vacío) es lo mismo que no mandar el parámetro. */
    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
