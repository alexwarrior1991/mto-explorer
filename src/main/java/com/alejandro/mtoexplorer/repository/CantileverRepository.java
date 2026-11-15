package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CantileverRepository implements PanacheRepository<Cantilever> {

    /** Las ménsulas de un perfil, por su posición (slot). */
    public Uni<List<Cantilever>> listByProfile(String ep, String track, int profileOrder) {
        return list("ep = ?1 and track = ?2 and profileOrder = ?3", Sort.by("slot"), ep, track, profileOrder);
    }

    /** Por paquete: ménsulas y ménsulas marcadas REVISAR. */
    public Uni<List<EpCount>> countPerEp() {
        return getSession().chain(session -> session.createSelectionQuery("""
                        select new com.alejandro.mtoexplorer.repository.projection.EpCount(
                            c.ep, count(c), sum(case when c.toReview then 1 else 0 end))
                        from Cantilever c group by c.ep""", EpCount.class)
                .getResultList());
    }
}
