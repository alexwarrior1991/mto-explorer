package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.dto.PageDto;
import com.alejandro.mtoexplorer.api.dto.ProfileDetailDto;
import com.alejandro.mtoexplorer.api.dto.ProfileSummaryDto;
import com.alejandro.mtoexplorer.api.mapper.DtoMapper;
import com.alejandro.mtoexplorer.kp.Kp;
import com.alejandro.mtoexplorer.repository.criteria.ProfileSearch;
import com.alejandro.mtoexplorer.service.ProfileService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

import static com.alejandro.mtoexplorer.api.QueryParams.blankToNull;

@Path("/api/profiles")
@Tag(name = "Perfiles", description = "Postes y pórticos, con sus ménsulas")
public class ProfileResource {

    private final ProfileService service;

    ProfileResource(ProfileService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "Perfiles de un paquete, opcionalmente de una vía y un tramo de PK, por vía y orden")
    public Uni<PageDto<ProfileSummaryDto>> search(
            @QueryParam("ep") @NotBlank String ep,
            @QueryParam("track") String track,
            @Parameter(description = "PK inicial: 12+345 o metros") @QueryParam("kpFrom") String kpFrom,
            @Parameter(description = "PK final: 12+345 o metros") @QueryParam("kpTo") String kpTo,
            @Parameter(description = "Solo los marcados REVISAR = SI") @QueryParam("toReview") Boolean toReview,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("50") @Min(1) @Max(500) int size) {
        return service.search(criteria(ep, track, kpFrom, kpTo, toReview), page, size)
                .map(result -> DtoMapper.toDto(result, DtoMapper::toSummary));
    }

    /**
     * Los mismos perfiles que la búsqueda, sin paginar y en streaming: NDJSON, un JSON por línea,
     * que salen según llegan de la base de datos. Devolver un {@link Multi} es lo que lo hace
     * streaming: Quarkus escribe cada elemento en cuanto se emite.
     */
    @GET
    @Path("/stream")
    @Produces(RestMediaType.APPLICATION_NDJSON)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(summary = "Todos los perfiles de la búsqueda en streaming (NDJSON), sin paginar")
    public Multi<ProfileSummaryDto> stream(
            @QueryParam("ep") @NotBlank String ep,
            @QueryParam("track") String track,
            @QueryParam("kpFrom") String kpFrom,
            @QueryParam("kpTo") String kpTo,
            @QueryParam("toReview") Boolean toReview) {
        return service.stream(criteria(ep, track, kpFrom, kpTo, toReview))
                .onItem().transform(DtoMapper::toSummary);
    }

    @GET
    @Path("/{ep}/{track}/{order}")
    @Operation(summary = "Ficha de un perfil con sus ménsulas")
    public Uni<ProfileDetailDto> get(@PathParam("ep") String ep, @PathParam("track") String track,
                                     @PathParam("order") int order) {
        return service.get(ep, track, order).map(DtoMapper::toDto);
    }

    /** Traduce los parámetros HTTP a criterios: textos vacíos fuera y el PK de "12+345" a metros. */
    private static ProfileSearch criteria(String ep, String track, String kpFrom, String kpTo, Boolean toReview) {
        return new ProfileSearch(ep, blankToNull(track), kp(kpFrom), kp(kpTo), toReview);
    }

    private static Double kp(String text) {
        return text == null || text.isBlank() ? null : Kp.parse(text);
    }
}
