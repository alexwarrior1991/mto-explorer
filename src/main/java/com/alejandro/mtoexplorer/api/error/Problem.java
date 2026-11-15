package com.alejandro.mtoexplorer.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Cuerpo de un error, en el formato de RFC 9457 ({@code application/problem+json}), el mismo que
 * usa mto-configuration con el ProblemDetail de Spring.
 *
 * @param type       qué clase de error es (/problems/not-found…)
 * @param title      el resumen del tipo, igual para todos los errores de ese tipo
 * @param status     el código HTTP
 * @param detail     qué ha pasado en esta petición concreta
 * @param instance   la ruta que se pidió
 * @param timestamp  cuándo
 * @param violations los campos que no pasan la validación (solo en los de tipo validation)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(String type, String title, int status, String detail, String instance, Instant timestamp,
                      List<Violation> violations) {

    /** Un campo que no pasa la validación. */
    public record Violation(String field, String message) {
    }
}
