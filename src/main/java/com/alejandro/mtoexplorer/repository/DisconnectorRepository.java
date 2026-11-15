package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Disconnector;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DisconnectorRepository implements PanacheRepository<Disconnector> {

    /** Los de un paquete; la estación y la vía (la suya o la conectada) son opcionales. */
    public Uni<List<Disconnector>> search(String ep, String station, String track) {
        List<String> where = new ArrayList<>(List.of("ep = :ep"));
        Parameters params = Parameters.with("ep", ep);
        if (station != null) {
            where.add("station = :station");
            params.and("station", station);
        }
        if (track != null) {
            where.add("(track = :track or connectedTrack = :track)");
            params.and("track", track);
        }
        return list(String.join(" and ", where), Sort.by("station").and("name"), params);
    }
}
