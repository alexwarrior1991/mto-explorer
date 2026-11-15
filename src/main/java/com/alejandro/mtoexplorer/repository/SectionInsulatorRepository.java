package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.SectionInsulator;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SectionInsulatorRepository implements PanacheRepository<SectionInsulator> {

    /** Los de un paquete, opcionalmente de una estación, por estación y KP. */
    public Uni<List<SectionInsulator>> search(String ep, String station) {
        Sort sort = Sort.by("station").and("kp").and("name");
        QueryFilter filter = QueryFilter.create()
                .eq("ep", ep)
                .eq("station", station);
        return list(filter.hql(), sort, filter.params());
    }
}
