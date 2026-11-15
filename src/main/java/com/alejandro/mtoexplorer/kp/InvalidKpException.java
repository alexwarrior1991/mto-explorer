package com.alejandro.mtoexplorer.kp;

/** Un texto que no es un PK válido. La API lo convierte en un 400. */
public class InvalidKpException extends IllegalArgumentException {

    private final String value;

    public InvalidKpException(String value, String message, Throwable cause) {
        super(message, cause);
        this.value = value;
    }

    /** El texto tal cual llegó. */
    public String value() {
        return value;
    }
}
