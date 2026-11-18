package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.dto.EpQualityDto;
import com.alejandro.mtoexplorer.api.dto.UnrecognizedDto;
import com.alejandro.mtoexplorer.api.mapper.DtoMapper;
import com.alejandro.mtoexplorer.service.QualityService;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import jakarta.ws.rs.PathParam;

import java.util.List;

import static com.alejandro.mtoexplorer.api.QueryParams.blankToNull;

@Path("/api/quality")
@Tag(name = "Calidad", description = "Lo que queda por revisar o por mapear")
public class QualityResource {

    private final QualityService service;

    QualityResource(QualityService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "Por paquete: perfiles y ménsulas, cuántos quedan por revisar y cuántos valores sin reconocer")
    public Uni<List<EpQualityDto>> perExecutionPackage() {
        return service.perExecutionPackage().map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }

    @GET
    @Path("/{ep}")
    @Operation(summary = "La calidad de un paquete (cacheada por paquete)")
    public Uni<EpQualityDto> ofExecutionPackage(@PathParam("ep") String ep) {
        return service.ofExecutionPackage(ep).map(DtoMapper::toDto);
    }

    @GET
    @Path("/unrecognized")
    @Operation(summary = "Valores de la hoja NO_RECONOCIDO, filtrables por maestro (LOV o PROFILE) y paquete")
    public Uni<List<UnrecognizedDto>> unrecognized(@QueryParam("master") String master, @QueryParam("ep") String ep) {
        return service.unrecognized(blankToNull(master), blankToNull(ep))
                .map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }
}
