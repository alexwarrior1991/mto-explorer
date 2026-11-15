package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Station;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class StationRepository implements PanacheRepository<Station> {

    public Uni<List<Station>> listByEp(String ep) {
        return list("ep", Sort.by("name"), ep);
    }
}
