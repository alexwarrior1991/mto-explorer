package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import com.alejandro.mtoexplorer.repository.criteria.ProfileSearch;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProfileRepository implements PanacheRepository<Profile> {

    /** El perfil, o null si no existe. PROFILE_ID se repite en una vía: lo que lo identifica es el orden. */
    public Uni<Profile> findByTrackAndOrder(String ep, String track, int sortOrder) {
        return find("ep = ?1 and track = ?2 and sortOrder = ?3", ep, track, sortOrder).firstResult();
    }

    /** La consulta sin ejecutar, ordenada por vía y orden: quien la usa decide la página o el recuento. */
    public PanacheQuery<Profile> search(ProfileSearch search) {
        QueryFilter filter = QueryFilter.create()
                .eq("ep", search.ep())
                .eq("track", search.track())
                .where("kp >= :kpFrom", "kpFrom", search.kpFrom())
                .where("kp <= :kpTo", "kpTo", search.kpTo())
                .eq("toReview", search.toReview());
        return find(filter.hql(), Sort.by("track").and("sortOrder"), filter.params());
    }

    public Uni<Long> countByEp(String ep) {
        return count("ep", ep);
    }

    /** Por paquete: perfiles y perfiles marcados REVISAR. */
    public Uni<List<EpCount>> countPerEp() {
        return getSession().chain(session -> session.createSelectionQuery("""
                        select new com.alejandro.mtoexplorer.repository.projection.EpCount(
                            p.ep, count(p), sum(case when p.toReview then 1 else 0 end))
                        from Profile p group by p.ep""", EpCount.class)
                .getResultList());
    }
}
