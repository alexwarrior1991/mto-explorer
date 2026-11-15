package com.alejandro.mtoexplorer.load;

/** El Excel no tiene la forma que se espera de un maestro. */
public class MasterFormatException extends RuntimeException {
    public MasterFormatException(String message) {
        super(message);
    }

    public MasterFormatException(String column, int row, String problem) {
        super("Columna " + column + ", fila " + row + ": " + problem);
    }
}
