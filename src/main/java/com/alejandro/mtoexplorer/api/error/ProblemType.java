package com.alejandro.mtoexplorer.api.error;

/**
 * Los tipos de error de la API. Cada uno da el {@code type}, el {@code title} y el {@code status}
 * de su respuesta: un cliente puede decidir qué hacer mirando el {@code type}, sin leer textos.
 */
public enum ProblemType {

    NOT_FOUND("not-found", "Recurso no encontrado", 404),
    INVALID_PARAMETER("invalid-parameter", "Parámetro no válido", 400),
    VALIDATION("validation", "La petición no es válida", 400),
    INVALID_MASTER("invalid-master", "El maestro no tiene la forma esperada", 422),
    HTTP("http", "Error HTTP", 0),
    INTERNAL("internal", "Error interno", 500);

    private final String slug;
    private final String title;
    private final int status;

    ProblemType(String slug, String title, int status) {
        this.slug = slug;
        this.title = title;
        this.status = status;
    }

    /** URI relativa que identifica el tipo, como pide RFC 9457: /problems/not-found. */
    public String type() {
        return "/problems/" + slug;
    }

    public String title() {
        return title;
    }

    public int status() {
        return status;
    }
}
