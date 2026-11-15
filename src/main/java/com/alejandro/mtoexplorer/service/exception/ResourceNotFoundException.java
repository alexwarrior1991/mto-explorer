package com.alejandro.mtoexplorer.service.exception;

/** Lo que se pide no existe. La API la convierte en un 404. */
public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final String id;

    public ResourceNotFoundException(String resource, String id) {
        super("No existe " + resource + " " + id);
        this.resource = resource;
        this.id = id;
    }

    public String resource() {
        return resource;
    }

    public String id() {
        return id;
    }
}
