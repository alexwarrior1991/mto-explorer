package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.SectionInsulator;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SectionInsulatorRepository implements PanacheRepository<SectionInsulator> {

    /** Los de un paquete, opcionalmente de una estación, por estación y KP. */
    public Uni<List<SectionInsulator>> search(String ep, String station) {
        Sort sort = Sort.by("station").and("kp").and("name");
        return station == null
                ? list("ep", sort, ep)
                : list("ep = :ep and station = :station", sort, Parameters.with("ep", ep).and("station", station));
    }
}
