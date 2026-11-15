package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.dto.ImportResultDto;
import com.alejandro.mtoexplorer.api.dto.MasterLoadDto;
import com.alejandro.mtoexplorer.api.mapper.DtoMapper;
import com.alejandro.mtoexplorer.imports.ImportHistoryService;
import com.alejandro.mtoexplorer.imports.MasterImportService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/loads")
@Tag(name = "Cargas", description = "Importación de los maestros y su historial")
public class LoadResource {

    private final MasterImportService imports;
    private final ImportHistoryService history;

    LoadResource(MasterImportService imports, ImportHistoryService history) {
        this.imports = imports;
        this.history = history;
    }

    @GET
    @Operation(summary = "Últimas cargas de los maestros")
    public Uni<List<MasterLoadDto>> recent() {
        return history.recent(20).map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }

    /** Bloqueante: lee Excel. {@code @Blocking} lo saca del event loop a un hilo worker. */
    @POST
    @Blocking
    @Operation(summary = "Vuelve a importar los maestros; con force=false solo los que han cambiado")
    public List<ImportResultDto> reload(@QueryParam("force") @DefaultValue("true") boolean force) {
        return DtoMapper.toDtos(force ? imports.importAll() : imports.importIfChanged(), DtoMapper::toDto);
    }
}
