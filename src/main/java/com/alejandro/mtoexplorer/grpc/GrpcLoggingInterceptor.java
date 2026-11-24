package com.alejandro.mtoexplorer.grpc;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Escribe en el log cada llamada gRPC al terminar: el método, su estado, cuántos mensajes ha
 * enviado y cuánto ha tardado. Ejemplo:
 * {@code gRPC mto.explorer.v1.Infrastructure/StreamProfiles: OK, 5 mensajes en 12 ms}. Si el
 * cliente corta antes (un stream que no termina, como WatchImports), el servidor no cierra la
 * llamada: se entera por {@code onCancel} de su listener.
 *
 * <p>Un {@link ServerInterceptor} envuelve la llamada antes de que llegue al servicio, como un
 * filtro de HTTP. {@link GlobalInterceptor} lo aplica a todos los servicios; sin ella, habría que
 * ponerlo en cada uno con {@code @RegisterInterceptor}.
 */
@GlobalInterceptor
@ApplicationScoped
public class GrpcLoggingInterceptor implements ServerInterceptor {

    private static final Logger LOG = Logger.getLogger(GrpcLoggingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        String method = call.getMethodDescriptor().getFullMethodName();
        long start = System.nanoTime();
        ServerCall<ReqT, RespT> logged = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            // gRPC llama a sendMessage y a close de uno en uno: no hace falta un AtomicInteger.
            private int sent;

            @Override
            public void sendMessage(RespT message) {
                sent++;
                super.sendMessage(message);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                LOG.infof("gRPC %s: %s, %d mensajes en %d ms", method, status.getCode(), sent, millisSince(start));
                super.close(status, trailers);
            }
        };
        ServerCall.Listener<ReqT> listener = next.startCall(logged, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onCancel() {
                LOG.infof("gRPC %s: cancelada por el cliente tras %d ms", method, millisSince(start));
                super.onCancel();
            }
        };
    }

    private static long millisSince(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
