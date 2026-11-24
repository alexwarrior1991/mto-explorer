package com.alejandro.mtoexplorer.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.quarkus.grpc.ExceptionHandler;

/**
 * Cierra la llamada con el {@code Status} de una excepción lanzada fuera de los {@code Uni} y
 * {@code Multi} del servicio (al recibir un mensaje, en un interceptor…). Lo crea
 * {@link GrpcExceptionMapper#createHandler} para cada llamada.
 */
final class GrpcExceptionHandler<ReqT, RespT> extends ExceptionHandler<ReqT, RespT> {

    GrpcExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> call, Metadata metadata) {
        super(listener, call, metadata);
    }

    @Override
    protected void handleException(Throwable failure, ServerCall<ReqT, RespT> call, Metadata metadata) {
        try {
            call.close(GrpcExceptionMapper.toStatus(failure), new Metadata());
        } catch (IllegalStateException alreadyClosed) {
            // La llamada ya estaba cerrada (el fallo llegó después de responder): no hay a quién
            // avisar, y volver a cerrarla haría saltar otra excepción encima de la primera.
        }
    }
}
