package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CantileverRepository implements PanacheRepository<Cantilever> {

    /** Por paquete: ménsulas y ménsulas marcadas REVISAR. */
    public Uni<List<EpCount>> countPerEp() {
        return getSession().chain(session -> session.createSelectionQuery("""
                        select new com.alejandro.mtoexplorer.repository.projection.EpCount(
                            e.code, count(c), sum(case when c.toReview then 1 else 0 end))
                        from Cantilever c join c.profile p join p.track t join t.executionPackage e
                        group by e.code""", EpCount.class)
                .getResultList());
    }
}
