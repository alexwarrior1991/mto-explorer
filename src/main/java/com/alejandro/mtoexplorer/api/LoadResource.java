package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.dto.ImportEventDto;
import com.alejandro.mtoexplorer.api.dto.ImportResultDto;
import com.alejandro.mtoexplorer.api.dto.MasterLoadDto;
import com.alejandro.mtoexplorer.api.mapper.DtoMapper;
import com.alejandro.mtoexplorer.imports.ImportHistoryService;
import com.alejandro.mtoexplorer.imports.MasterImportService;
import com.alejandro.mtoexplorer.imports.event.ImportFeed;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Duration;
import java.util.List;

@Path("/api/loads")
@Tag(name = "Cargas", description = "Importación de los maestros y su historial")
public class LoadResource {

    /**
     * Cada cuánto se manda el latido por las conexiones SSE abiertas.
     */
    static final long HEARTBEAT_SECONDS = 25;

    private final MasterImportService imports;
    private final ImportHistoryService history;
    private final ImportFeed feed;

    LoadResource(MasterImportService imports, ImportHistoryService history, ImportFeed feed) {
        this.imports = imports;
        this.history = history;
        this.feed = feed;
    }

    @GET
    @Operation(summary = "Últimas cargas de los maestros")
    public Uni<List<MasterLoadDto>> recent() {
        return history.recent(20).map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }

    /**
     * Server-Sent Events: la conexión se queda abierta y el servidor manda un evento cada vez que se
     * importa un fichero, desde cualquier sitio (el arranque, POST /api/loads, el Scheduler). En el
     * navegador: {@code new EventSource("/api/loads/events").addEventListener("import", ...)}; en la
     * terminal: {@code curl -N http://localhost:8090/api/loads/events}.
     *
     * <p>Tres flujos juntos con {@code Multi.createBy().merging()}, que se suscribe a todos a la vez:
     * <ul>
     *   <li>{@code import}: un evento por fichero importado, del canal {@link ImportFeed}.
     *   <li>{@code connected}: uno al principio. Quarkus no manda las cabeceras HTTP hasta el primer
     *       evento; sin este, quien se conecta no sabe si está conectado hasta la primera importación.
     *       Va después del canal en la lista: cuando el cliente lo recibe, ya está suscrito y no se
     *       pierde nada.
     *   <li>un comentario cada {@value #HEARTBEAT_SECONDS} s: los proxies cortan las conexiones que
     *       llevan un rato sin tráfico. Un comentario SSE (": ping") no es un evento para el cliente.
     * </ul>
     */
    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Las importaciones en directo (Server-Sent Events): un evento \"import\" por fichero")
    public Multi<OutboundSseEvent> events(@Context Sse sse) {
        Multi<OutboundSseEvent> imports = feed.events().map(event -> sse.newEventBuilder()
                .name("import")
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .data(ImportEventDto.class, DtoMapper.toDto(event))
                .build()
        );

        Multi<OutboundSseEvent> connected = Multi.createFrom().item(() ->
                sse.newEventBuilder()
                        .name("connected")
                        .data("ok")
                        .build()
        );

        Multi<OutboundSseEvent> heartbeat = Multi.createFrom().ticks()
                .startingAfter(Duration.ofSeconds(HEARTBEAT_SECONDS))
                .every(Duration.ofSeconds(HEARTBEAT_SECONDS))
                .map(tick -> sse.newEventBuilder().comment("ping").build());

        return Multi.createBy().merging().streams(imports, connected, heartbeat);
    }

    /**
     * Bloqueante: lee Excel. {@code @Blocking} lo saca del event loop a un hilo worker.
     */
    @POST
    @Blocking
    @Operation(summary = "Vuelve a importar los maestros; con force=false solo los que han cambiado")
    public List<ImportResultDto> reload(@QueryParam("force") @DefaultValue("true") boolean force) {
        return DtoMapper.toDtos(force ? imports.importAll() : imports.importIfChanged(), DtoMapper::toDto);
    }
}
