package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.SectionInsulator;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SectionInsulatorRepository implements PanacheRepository<SectionInsulator> {

    /**
     * Los de un paquete, opcionalmente de una estación, con sus agujas: una sola consulta con
     * {@code left join fetch} de la colección {@code switches} (que se ordena por su {@code @OrderBy}).
     * Hibernate quita los aisladores repetidos que produce el join con la colección.
     */
    public Uni<List<SectionInsulator>> search(String ep, String station) {
        QueryFilter filter = QueryFilter.create()
                .where("e.code = :ep", "ep", ep)
                .where("s.name = :station", "station", station);
        return list("""
                select i from SectionInsulator i
                join fetch i.station s
                join fetch s.executionPackage e
                left join fetch i.track
                left join fetch i.connectedTrack
                left join fetch i.switches w
                left join fetch w.track
                where %s
                order by s.name, i.kp, i.name""".formatted(filter.hql()), filter.params());
    }
}
