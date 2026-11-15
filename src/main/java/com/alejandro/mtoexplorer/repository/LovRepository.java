package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Lov;
import com.alejandro.mtoexplorer.repository.projection.LovFamilyCount;
import com.alejandro.mtoexplorer.repository.criteria.LovSearch;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class LovRepository implements PanacheRepository<Lov> {

    /** La consulta sin ejecutar, ordenada por familia y código. */
    public PanacheQuery<Lov> search(LovSearch search) {
        Sort sort = Sort.by("entity").and("code");
        List<String> where = new ArrayList<>();
        Parameters params = new Parameters();
        if (search.entity() != null) {
            where.add("entity = :entity");
            params.and("entity", search.entity());
        }
        if (search.text() != null) {
            where.add("(lower(code) like :text or lower(descriptionEn) like :text or lower(descriptionEs) like :text)");
            params.and("text", "%" + search.text().toLowerCase() + "%");
        }
        return where.isEmpty() ? findAll(sort) : find(String.join(" and ", where), sort, params);
    }

    public Uni<List<LovFamilyCount>> countPerFamily() {
        return getSession().chain(session -> session.createSelectionQuery("""
                        select new com.alejandro.mtoexplorer.repository.projection.LovFamilyCount(
                            l.entity, count(l), sum(case when l.toReview then 1 else 0 end))
                        from Lov l group by l.entity order by l.entity""", LovFamilyCount.class)
                .getResultList());
    }
}
