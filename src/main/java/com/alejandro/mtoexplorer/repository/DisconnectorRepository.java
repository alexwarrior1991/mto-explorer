package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class DisconnectorRepository implements PanacheRepository<Disconnector> {

    /** Cómo se llama la "estación" de un seccionador que no tiene: como en el maestro. */
    public static final String NO_STATION = "SIN ESTACION";

    /**
     * Los de un paquete con todo lo que enseña la API ya cargado. Las relaciones opcionales van con
     * {@code left join fetch}: con un {@code join} a secas, un seccionador sin estación o sin poste
     * desaparecería del resultado.
     */
    public Uni<List<Disconnector>> search(String ep, String station, String track) {
        QueryFilter filter = QueryFilter.create()
                .where("e.code = :ep", "ep", ep)
                .where("coalesce(s.name, '" + NO_STATION + "') = :station", "station", station)
                .where("(t.name = :track or ct.name = :track)", "track", track);
        return list("""
                select d from Disconnector d
                join fetch d.executionPackage e
                left join fetch d.station s
                left join fetch d.track t
                left join fetch d.connectedTrack ct
                left join fetch d.pole p
                left join fetch p.track
                where %s
                order by coalesce(s.name, '%s'), d.name""".formatted(filter.hql(), NO_STATION), filter.params());
    }
}
