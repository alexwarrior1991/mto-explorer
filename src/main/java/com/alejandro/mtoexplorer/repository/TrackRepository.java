package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Track;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TrackRepository implements PanacheRepository<Track> {

    public Uni<List<Track>> listByEp(String ep) {
        return list("ep", Sort.by("name"), ep);
    }

    public Uni<Long> countByEp(String ep) {
        return count("ep", ep);
    }
}
