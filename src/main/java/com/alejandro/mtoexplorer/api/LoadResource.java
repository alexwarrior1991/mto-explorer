package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.entity.MasterLoad;
import com.alejandro.mtoexplorer.load.FileLoadResult;
import com.alejandro.mtoexplorer.load.MasterLoader;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.time.Instant;
import java.util.List;

@Path("/api/loads")
public class LoadResource {

    public record MasterLoadDto(String file, Instant lastModified, long sizeBytes, Instant loadedAt,
                                int rows, long durationMs) {
    }

    private final MasterLoader loader;

    LoadResource(MasterLoader loader) {
        this.loader = loader;
    }

    @GET
    @WithSession
    @Operation(summary = "Últimas cargas de los maestros")
    public Uni<List<MasterLoadDto>> recent() {
        return MasterLoad.recent(20).map(loads -> loads.stream()
                .map(l -> new MasterLoadDto(l.fileName, l.lastModified, l.sizeBytes, l.loadedAt, l.rowCount, l.durationMs))
                .toList());
    }

    /** Bloqueante: lee Excel. @Blocking lo saca del event loop a un hilo worker. */
    @POST
    @Blocking
    @Operation(summary = "Vuelve a cargar los maestros; con force=false solo los que han cambiado")
    public List<FileLoadResult> reload(@QueryParam("force") @DefaultValue("true") boolean force) {
        return force ? loader.reloadAll() : loader.reloadIfChanged();
    }
}
