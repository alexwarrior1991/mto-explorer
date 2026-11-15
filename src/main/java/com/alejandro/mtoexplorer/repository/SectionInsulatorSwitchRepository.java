package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SectionInsulatorSwitchRepository implements PanacheRepository<SectionInsulatorSwitch> {

    /** Las agujas de todos los aisladores de un paquete, opcionalmente de una estación, por KP y código. */
    public Uni<List<SectionInsulatorSwitch>> search(String ep, String station) {
        Sort sort = Sort.by("kp").and("code");
        return station == null
                ? list("ep", sort, ep)
                : list("ep = :ep and station = :station", sort, Parameters.with("ep", ep).and("station", station));
    }
}
