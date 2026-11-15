package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.Dtos.ProfileDetail;
import com.alejandro.mtoexplorer.api.Dtos.ProfileSummary;
import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.kp.Kp;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.util.ArrayList;
import java.util.List;

@Path("/api/profiles")
@WithSession
public class ProfileResource {

    @GET
    @Operation(summary = "Perfiles de un paquete, opcionalmente de una vía y un tramo de PK, ordenados por vía y orden")
    public Uni<PageResult<ProfileSummary>> search(
            @QueryParam("ep") @NotBlank String ep,
            @QueryParam("track") String track,
            @Parameter(description = "PK inicial: 12+345 o metros") @QueryParam("kpFrom") String kpFrom,
            @Parameter(description = "PK final: 12+345 o metros") @QueryParam("kpTo") String kpTo,
            @Parameter(description = "Solo los marcados REVISAR = SI") @QueryParam("toReview") Boolean toReview,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("50") @Min(1) @Max(500) int size) {

        List<String> where = new ArrayList<>(List.of("ep = :ep"));
        Parameters params = Parameters.with("ep", ep);
        if (track != null && !track.isBlank()) {
            where.add("track = :track");
            params.and("track", track);
        }
        if (kpFrom != null && !kpFrom.isBlank()) {
            where.add("kp >= :kpFrom");
            params.and("kpFrom", Kp.parse(kpFrom));
        }
        if (kpTo != null && !kpTo.isBlank()) {
            where.add("kp <= :kpTo");
            params.and("kpTo", Kp.parse(kpTo));
        }
        if (toReview != null) {
            where.add("toReview = :toReview");
            params.and("toReview", toReview);
        }

        PanacheQuery<Profile> query = Profile.find(String.join(" and ", where),
                Sort.by("track").and("sortOrder"), params);
        // Una sesión reactiva no admite dos consultas a la vez: primero la página, después el total.
        return query.page(page, size).list()
                .chain(profiles -> query.count().map(total -> new PageResult<>(
                        profiles.stream().map(ProfileSummary::of).toList(), page, size, total)));
    }

    @GET
    @Path("/{ep}/{track}/{order}")
    @Operation(summary = "Ficha de un perfil con sus ménsulas")
    public Uni<ProfileDetail> get(@PathParam("ep") String ep, @PathParam("track") String track,
                                  @PathParam("order") int order) {
        return Profile.findByTrackAndOrder(ep, track, order)
                .onItem().ifNull().failWith(() ->
                        new NotFoundException("No existe el perfil " + order + " de " + ep + " / " + track))
                .chain(profile -> Cantilever.listByProfile(ep, track, order)
                        .map(cantilevers -> ProfileDetail.of(profile, cantilevers)));
    }
}
