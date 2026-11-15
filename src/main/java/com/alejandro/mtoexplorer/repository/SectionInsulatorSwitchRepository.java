package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SectionInsulatorSwitchRepository implements PanacheRepository<SectionInsulatorSwitch> {

    /** Las agujas de todos los aisladores de un paquete, opcionalmente de una estación, por KP y código. */
    public Uni<List<SectionInsulatorSwitch>> search(String ep, String station) {
        Sort sort = Sort.by("kp").and("code");
        QueryFilter filter = QueryFilter.create()
                .eq("ep", ep)
                .eq("station", station);
        return list(filter.hql(), sort, filter.params());
    }
}
