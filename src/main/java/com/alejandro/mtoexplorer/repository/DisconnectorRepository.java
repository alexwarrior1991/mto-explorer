package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class DisconnectorRepository implements PanacheRepository<Disconnector> {

    /** Los de un paquete; la estación y la vía (la suya o la conectada) son opcionales. */
    public Uni<List<Disconnector>> search(String ep, String station, String track) {
        QueryFilter filter = QueryFilter.create()
                .eq("ep", ep)
                .eq("station", station)
                .where("(track = :track or connectedTrack = :track)", "track", track);
        return list(filter.hql(), Sort.by("station").and("name"), filter.params());
    }
}
