package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.Dtos.DisconnectorDto;
import com.alejandro.mtoexplorer.api.Dtos.SectionInsulatorDto;
import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.SectionInsulator;
import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** El seccionamiento de un paquete: seccionadores y aisladores de sección con sus agujas. */
@Path("/api/sectioning")
@WithSession
public class SectioningResource {

    @GET
    @Path("/disconnectors")
    @Operation(summary = "Seccionadores de un paquete, opcionalmente de una estación o de una vía")
    public Uni<List<DisconnectorDto>> disconnectors(@QueryParam("ep") @NotBlank String ep,
                                                    @QueryParam("station") String station,
                                                    @QueryParam("track") String track) {
        List<String> where = new ArrayList<>(List.of("ep = :ep"));
        Parameters params = Parameters.with("ep", ep);
        if (station != null && !station.isBlank()) {
            where.add("station = :station");
            params.and("station", station);
        }
        if (track != null && !track.isBlank()) {
            where.add("(track = :track or connectedTrack = :track)");
            params.and("track", track);
        }
        return Disconnector.<Disconnector>list(String.join(" and ", where), Sort.by("station").and("name"), params)
                .map(list -> list.stream().map(DisconnectorDto::of).toList());
    }

    @GET
    @Path("/section-insulators")
    @Operation(summary = "Aisladores de sección de un paquete, con sus agujas; opcionalmente de una estación")
    public Uni<List<SectionInsulatorDto>> sectionInsulators(@QueryParam("ep") @NotBlank String ep,
                                                            @QueryParam("station") String station) {
        String where = "ep = :ep";
        Parameters params = Parameters.with("ep", ep);
        if (station != null && !station.isBlank()) {
            where += " and station = :station";
            params.and("station", station);
        }
        String query = where;
        // Dos consultas seguidas en la misma sesión (no a la vez): los aisladores y, después, todas sus agujas.
        return SectionInsulator.<SectionInsulator>list(query, Sort.by("station").and("kp").and("name"), params)
                .chain(insulators -> SectionInsulatorSwitch.<SectionInsulatorSwitch>list(query,
                                Sort.by("kp").and("code"), params)
                        .map(switches -> {
                            Map<String, List<SectionInsulatorSwitch>> byInsulator = switches.stream()
                                    .collect(Collectors.groupingBy(w -> w.station + "|" + w.insulator));
                            return insulators.stream()
                                    .map(i -> SectionInsulatorDto.of(i,
                                            byInsulator.getOrDefault(i.station + "|" + i.name, List.of())))
                                    .toList();
                        }));
    }
}
