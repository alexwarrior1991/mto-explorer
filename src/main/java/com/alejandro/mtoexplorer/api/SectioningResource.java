package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.dto.DisconnectorDto;
import com.alejandro.mtoexplorer.api.dto.SectionInsulatorDto;
import com.alejandro.mtoexplorer.api.mapper.DtoMapper;
import com.alejandro.mtoexplorer.service.SectioningService;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

import static com.alejandro.mtoexplorer.api.QueryParams.blankToNull;

@Path("/api/sectioning")
@Tag(name = "Seccionamiento", description = "Seccionadores y aisladores de sección con sus agujas")
public class SectioningResource {

    private final SectioningService service;

    SectioningResource(SectioningService service) {
        this.service = service;
    }

    @GET
    @Path("/disconnectors")
    @Operation(summary = "Seccionadores de un paquete, opcionalmente de una estación o de una vía")
    public Uni<List<DisconnectorDto>> disconnectors(@QueryParam("ep") @NotBlank String ep,
                                                    @QueryParam("station") String station,
                                                    @QueryParam("track") String track) {
        return service.disconnectors(ep, blankToNull(station), blankToNull(track))
                .map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }

    @GET
    @Path("/section-insulators")
    @Operation(summary = "Aisladores de sección de un paquete, con sus agujas; opcionalmente de una estación")
    public Uni<List<SectionInsulatorDto>> sectionInsulators(@QueryParam("ep") @NotBlank String ep,
                                                            @QueryParam("station") String station) {
        return service.sectionInsulators(ep, blankToNull(station))
                .map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }
}
