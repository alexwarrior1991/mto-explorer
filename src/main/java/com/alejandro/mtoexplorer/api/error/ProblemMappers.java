package com.alejandro.mtoexplorer.api.error;

import com.alejandro.mtoexplorer.imports.excel.MasterFormatException;
import com.alejandro.mtoexplorer.kp.InvalidKpException;
import com.alejandro.mtoexplorer.service.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Convierte cada excepción en una respuesta {@code application/problem+json}. Es el único sitio
 * que sabe de códigos HTTP: los servicios lanzan excepciones de negocio
 * ({@link ResourceNotFoundException}…) y no saben que hay una API delante.
 *
 * <p>Quarkus elige el mapper de la excepción más concreta, así que {@link #unexpected} solo
 * recoge lo que ningún otro reconoce.
 */
public class ProblemMappers {

    private static final Logger LOG = Logger.getLogger(ProblemMappers.class);
    static final MediaType PROBLEM_JSON = new MediaType("application", "problem+json");

    @ServerExceptionMapper
    public Response notFound(ResourceNotFoundException e, UriInfo uri) {
        return problem(ProblemType.NOT_FOUND, ProblemType.NOT_FOUND.status(), e.getMessage(), uri, null);
    }

    @ServerExceptionMapper
    public Response invalidKp(InvalidKpException e, UriInfo uri) {
        return problem(ProblemType.INVALID_PARAMETER, ProblemType.INVALID_PARAMETER.status(), e.getMessage(), uri,
                null);
    }

    /** La validación de los parámetros (@NotBlank, @Min, @Max…): un 400 con la lista de campos que fallan. */
    @ServerExceptionMapper
    public Response validation(ConstraintViolationException e, UriInfo uri) {
        List<Problem.Violation> violations = e.getConstraintViolations().stream()
                .map(v -> new Problem.Violation(field(v), v.getMessage()))
                .sorted(Comparator.comparing(Problem.Violation::field))
                .toList();
        return problem(ProblemType.VALIDATION, ProblemType.VALIDATION.status(),
                violations.size() + " parámetro(s) no válido(s)", uri, violations);
    }

    @ServerExceptionMapper
    public Response invalidMaster(MasterFormatException e, UriInfo uri) {
        return problem(ProblemType.INVALID_MASTER, ProblemType.INVALID_MASTER.status(), e.getMessage(), uri, null);
    }

    /** Las de JAX-RS (ruta inexistente, método no permitido…): se respeta su código. */
    @ServerExceptionMapper
    public Response http(WebApplicationException e, UriInfo uri) {
        int status = e.getResponse().getStatus();
        return problem(ProblemType.HTTP, status, e.getMessage(), uri, null);
    }

    /** Lo inesperado: un 500 que no enseña el detalle (puede llevar datos internos) y se escribe en el log. */
    @ServerExceptionMapper
    public Response unexpected(Exception e, UriInfo uri) {
        LOG.errorf(e, "Error no controlado en %s", uri.getPath());
        return problem(ProblemType.INTERNAL, ProblemType.INTERNAL.status(),
                "Ha ocurrido un error inesperado", uri, null);
    }

    private static Response problem(ProblemType type, int status, String detail, UriInfo uri,
                                    List<Problem.Violation> violations) {
        Problem body = new Problem(type.type(), title(type, status), status, detail, "/" + uri.getPath()
                .replaceFirst("^/", ""), Instant.now(), violations);
        return Response.status(status).type(PROBLEM_JSON).entity(body).build();
    }

    /** El de su tipo; en los de JAX-RS, la frase del código HTTP ("Not Found", "Method Not Allowed"…). */
    private static String title(ProblemType type, int status) {
        Response.Status known = Response.Status.fromStatusCode(status);
        return type == ProblemType.HTTP && known != null ? known.getReasonPhrase() : type.title();
    }

    /** "search.ep" → "ep": el último tramo de la ruta de la propiedad es el nombre del parámetro. */
    private static String field(ConstraintViolation<?> violation) {
        String field = null;
        for (Path.Node node : violation.getPropertyPath()) {
            field = node.getName();
        }
        return field;
    }
}
