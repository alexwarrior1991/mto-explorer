package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Track;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TrackRepository implements PanacheRepository<Track> {

    /**
     * Las vías de un paquete con sus estaciones, en una sola consulta: {@code left join fetch} de la
     * colección {@code @ManyToMany} (left, porque hay vías sin estaciones). Sin el fetch, leer
     * {@code track.stations} fallaría; con {@code Mutiny.fetch} por vía serían N consultas más.
     */
    public Uni<List<Track>> listByEp(String ep) {
        return list("""
                select t from Track t
                join fetch t.executionPackage e
                left join fetch t.stations
                where e.code = ?1
                order by t.name""", ep);
    }

    public Uni<Long> countByEp(String ep) {
        return count("executionPackage.code", ep);
    }
}
