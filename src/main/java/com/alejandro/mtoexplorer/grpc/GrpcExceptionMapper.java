package com.alejandro.mtoexplorer.grpc;

import com.alejandro.mtoexplorer.kp.InvalidKpException;
import com.alejandro.mtoexplorer.service.exception.ResourceNotFoundException;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.grpc.ExceptionHandlerProvider;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Convierte cada excepción en un {@link Status} de gRPC. Es el {@code ProblemMappers} de gRPC: los
 * servicios lanzan excepciones de negocio y no saben que hay gRPC delante.
 *
 * <p>Quarkus tiene un {@link ExceptionHandlerProvider} por defecto que convierte todo lo que no es
 * un {@code StatusException} en UNKNOWN. Declarar un bean que implementa la interfaz lo sustituye.
 * Tiene dos entradas:
 * <ul>
 *   <li>{@link #transform}: los fallos de los {@code Uni} y {@code Multi} que devuelve el servicio.
 *       Es lo normal en este proyecto.
 *   <li>{@link #createHandler}: las excepciones que se lanzan fuera de ellos, al recibir el mensaje.
 * </ul>
 */
@ApplicationScoped
public class GrpcExceptionMapper implements ExceptionHandlerProvider {

    private static final Logger LOG = Logger.getLogger(GrpcExceptionMapper.class);

    @Override
    public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(
            ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata) {
        return new GrpcExceptionHandler<>(listener, serverCall, metadata);
    }

    @Override
    public Throwable transform(Throwable failure) {
        return toStatus(failure).asException();
    }

    /**
     * NOT_FOUND, INVALID_ARGUMENT… o INTERNAL sin detalle (puede llevar datos internos) y al log.
     */
    static Status toStatus(Throwable failure) {
        return switch (failure) {
            case StatusException e -> e.getStatus();
            case StatusRuntimeException e -> e.getStatus();
            case ResourceNotFoundException e -> Status.NOT_FOUND.withDescription(e.getMessage());
            case InvalidKpException e -> Status.INVALID_ARGUMENT.withDescription(e.getMessage());
            default -> {
                LOG.error("Error no controlado en una llamada gRPC", failure);
                yield Status.INTERNAL.withDescription("Ha ocurrido un error inesperado");
            }
        };
    }
}
