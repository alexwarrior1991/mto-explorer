package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.repository.criteria.ProfileSearch;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProfileRepository implements PanacheRepository<Profile> {

    /** El perfil con su vía y su paquete ya cargados, o null si no existe. */
    public Uni<Profile> findByTrackAndOrder(String ep, String track, int sortOrder) {
        return find("""
                from Profile p
                join fetch p.track t
                join fetch t.executionPackage e
                where e.code = ?1 and t.name = ?2 and p.sortOrder = ?3""", ep, track, sortOrder).firstResult();
    }

    /**
     * La búsqueda sin ejecutar, con la vía y el paquete de cada perfil ya cargados, ordenada por vía y
     * orden: quien la usa decide la página. El total se pide aparte con {@link #count(ProfileSearch)}:
     * contar sobre una consulta con {@code join fetch} no tiene sentido.
     */
    public PanacheQuery<Profile> search(ProfileSearch search) {
        QueryFilter filter = filter(search);
        return find("from Profile p join fetch p.track t join fetch t.executionPackage e where " + filter.hql()
                + " order by t.name, p.sortOrder", filter.params());
    }

    public Uni<Long> count(ProfileSearch search) {
        QueryFilter filter = filter(search);
        return count("from Profile p join p.track t join t.executionPackage e where " + filter.hql(),
                filter.params());
    }

    /** Las condiciones de la búsqueda, con los alias de las dos consultas: p (perfil), t (vía), e (paquete). */
    private static QueryFilter filter(ProfileSearch search) {
        return QueryFilter.create()
                .where("e.code = :ep", "ep", search.ep())
                .where("t.name = :track", "track", search.track())
                .where("p.kp >= :kpFrom", "kpFrom", search.kpFrom())
                .where("p.kp <= :kpTo", "kpTo", search.kpTo())
                .where("p.toReview = :toReview", "toReview", search.toReview());
    }

    public Uni<Long> countByEp(String ep) {
        return count("track.executionPackage.code", ep);
    }

    /** Por paquete: perfiles y perfiles marcados REVISAR. */
    public Uni<List<EpCount>> countPerEp() {
        return getSession().chain(session -> session.createSelectionQuery("""
                        select new com.alejandro.mtoexplorer.repository.projection.EpCount(
                            e.code, count(p), sum(case when p.toReview then 1 else 0 end))
                        from Profile p join p.track t join t.executionPackage e group by e.code""", EpCount.class)
                .getResultList());
    }
}
