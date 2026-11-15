package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Lov;
import com.alejandro.mtoexplorer.repository.projection.LovFamilyCount;
import com.alejandro.mtoexplorer.repository.criteria.LovSearch;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class LovRepository implements PanacheRepository<Lov> {

    /** La consulta sin ejecutar, ordenada por familia y código. */
    public PanacheQuery<Lov> search(LovSearch search) {
        Sort sort = Sort.by("entity").and("code");
        QueryFilter filter = QueryFilter.create()
                .eq("entity", search.entity())
                .where("(lower(code) like :text or lower(descriptionEn) like :text or lower(descriptionEs) like :text)",
                        "text", search.text() == null ? null : "%" + search.text().toLowerCase() + "%");
        return filter.isEmpty() ? findAll(sort) : find(filter.hql(), sort, filter.params());
    }

    public Uni<List<LovFamilyCount>> countPerFamily() {
        return getSession().chain(session -> session.createSelectionQuery("""
                        select new com.alejandro.mtoexplorer.repository.projection.LovFamilyCount(
                            l.entity, count(l), sum(case when l.toReview then 1 else 0 end))
                        from Lov l group by l.entity order by l.entity""", LovFamilyCount.class)
                .getResultList());
    }
}
