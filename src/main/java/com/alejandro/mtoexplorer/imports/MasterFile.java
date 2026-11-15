package com.alejandro.mtoexplorer.imports;

import com.alejandro.mtoexplorer.entity.MasterLoad;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Un fichero maestro en disco y su huella (fecha de modificación y tamaño), que es lo que decide
 * si ha cambiado desde la última carga.
 */
record MasterFile(Path path, String name, Instant lastModified, long size) {

    static boolean exists(Path path) {
        return Files.isRegularFile(path);
    }

    static MasterFile of(Path path) {
        try {
            // PostgreSQL guarda microsegundos: truncar para que la comparación con la última carga sea estable.
            Instant lastModified = Files.getLastModifiedTime(path).toInstant().truncatedTo(ChronoUnit.MILLIS);
            return new MasterFile(path, path.getFileName().toString(), lastModified, Files.size(path));
        } catch (IOException e) {
            throw new UncheckedIOException("No se puede leer " + path, e);
        }
    }

    /** Si coincide con la huella de una carga anterior (null si no la hay). */
    boolean sameAs(MasterLoad last) {
        return last != null && last.sizeBytes == size && last.lastModified.equals(lastModified);
    }
}
