package com.alejandro.mtoexplorer.grpc;

import com.alejandro.mtoexplorer.grpc.v1.ExecutionPackageInfo;
import com.alejandro.mtoexplorer.grpc.v1.GetExecutionPackageRequest;
import com.alejandro.mtoexplorer.grpc.v1.ImportNotice;
import com.alejandro.mtoexplorer.grpc.v1.Infrastructure;
import com.alejandro.mtoexplorer.grpc.v1.ProfileInfo;
import com.alejandro.mtoexplorer.grpc.v1.ProfileKey;
import com.alejandro.mtoexplorer.grpc.v1.ProfileLookup;
import com.alejandro.mtoexplorer.grpc.v1.ProfileQuery;
import com.alejandro.mtoexplorer.grpc.v1.ProfileSummary;
import com.alejandro.mtoexplorer.grpc.v1.WatchImportsRequest;
import com.alejandro.mtoexplorer.imports.event.ImportFeed;
import com.alejandro.mtoexplorer.service.ExecutionPackageService;
import com.alejandro.mtoexplorer.service.ProfileService;
import com.alejandro.mtoexplorer.service.exception.ResourceNotFoundException;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;


/**
 * El servicio {@code Infrastructure} del .proto. Implementa la interfaz {@link Infrastructure} que
 * Quarkus genera para Mutiny: cada rpc es un método que devuelve un {@link Uni} (respuesta única) o
 * un {@link Multi} (stream), y el bidireccional además recibe un {@code Multi}.
 *
 * <p>Es la misma capa que un recurso REST: traduce la petición, llama a un servicio y convierte el
 * resultado ({@link ProtoMapper}). Los errores de negocio los convierte en {@code Status} un solo
 * sitio, {@link GrpcExceptionMapper}, como hace {@code ProblemMappers} para REST.
 */
@GrpcService
public class InfrastructureGrpcService implements Infrastructure {

    private final ExecutionPackageService executionPackages;
    private final ProfileService profiles;
    private final ImportFeed imports;

    InfrastructureGrpcService(ExecutionPackageService executionPackages, ProfileService profiles,
                              ImportFeed imports) {
        this.executionPackages = executionPackages;
        this.profiles = profiles;
        this.imports = imports;
    }

    /**
     * Unaria. Sale de la caché del resumen (Redis), como {@code GET /api/eps/overview}.
     */
    @Override
    public Uni<ExecutionPackageInfo> getExecutionPackage(GetExecutionPackageRequest request) {
        return executionPackages.overview(request.getCode()).map(ProtoMapper::toProto);
    }

    /**
     * Unaria. Un perfil que no existe falla con ResourceNotFoundException → NOT_FOUND.
     */
    @Override
    public Uni<ProfileInfo> getProfile(ProfileKey key) {
        return profiles.get(key.getEp(), key.getTrack(), key.getOrder()).map(ProtoMapper::toProto);
    }

    /**
     * Streaming del servidor: el mismo {@code Multi} que {@code GET /api/profiles/stream}. Cada
     * perfil sale por la conexión en cuanto llega su página.
     *
     * <p>{@code deferred}: la petición se traduce al suscribirse, dentro del {@code Multi}. Así, si
     * falla (un PK que no se entiende), el fallo viaja por el stream y lo convierte
     * {@link GrpcExceptionMapper}, en vez de salir como una excepción del método.
     */
    @Override
    public Multi<ProfileSummary> streamProfiles(ProfileQuery query) {
        return Multi.createFrom().deferred(() -> {
                    if (query.getEp().isBlank()) {
                        // Lo que en REST hace @NotBlank. Un StatusRuntimeException llega al cliente tal cual.
                        throw Status.INVALID_ARGUMENT.withDescription("ep es obligatorio").asRuntimeException();
                    }
                    return profiles.stream(ProtoMapper.toSearch(query));
                })
                .onItem().transform(ProtoMapper::toSummary);
    }

    /**
     * Streaming del servidor que no termina: el canal en directo de las importaciones (el mismo
     * {@code Multi} caliente que el SSE). Acaba cuando el cliente cancela.
     */
    @Override
    public Multi<ImportNotice> watchImports(WatchImportsRequest request) {
        return imports.events().onItem().transform(ProtoMapper::toProto);
    }

    /**
     * Bidireccional: por cada clave que manda el cliente, una respuesta, en el mismo orden.
     *
     * <p>{@code transformToUniAndConcatenate}: una consulta detrás de otra, como en el resumen de
     * paquetes. Una clave que no existe no corta el stream: se responde con {@code error} y se sigue
     * con la siguiente. Cualquier otro fallo sí lo corta.
     */
    @Override
    public Multi<ProfileLookup> lookupProfiles(Multi<ProfileKey> keys) {
        return keys.onItem().transformToUniAndConcatenate(key -> profiles
                .get(key.getEp(), key.getTrack(), key.getOrder())
                .map(detail -> ProtoMapper.found(key, detail))
                .onFailure(ResourceNotFoundException.class)
                .recoverWithItem(e -> ProtoMapper.notFound(key, e.getMessage()))
        );
    }
}
