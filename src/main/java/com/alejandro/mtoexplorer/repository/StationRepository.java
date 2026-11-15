package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Station;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class StationRepository implements PanacheRepository<Station> {

    /**
     * Las de un paquete, con su paquete ya cargado ({@code join fetch}): el DTO lee
     * {@code station.executionPackage.code}, y una relación LAZY sin cargar fallaría al leerla.
     */
    public Uni<List<Station>> listByEp(String ep) {
        return list("from Station s join fetch s.executionPackage e where e.code = ?1 order by s.name", ep);
    }
}
