package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.UnrecognizedValue;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import com.alejandro.mtoexplorer.repository.support.QueryFilter;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class UnrecognizedValueRepository implements PanacheRepository<UnrecognizedValue> {

    /** Los valores de un maestro (LOV o PROFILE) y de un paquete; los dos filtros son opcionales. */
    public Uni<List<UnrecognizedValue>> search(String master, String ep) {
        Sort sort = Sort.by("master").and("ep").and("kind").and("value");
        QueryFilter filter = QueryFilter.create()
                .eq("master", master)
                .eq("ep", ep);
        return filter.isEmpty() ? listAll(sort) : list(filter.hql(), sort, filter.params());
    }

    public Uni<Long> deleteByMaster(String master) {
        return delete("master", master);
    }

    /** Por paquete: cuántos valores no reconocidos hay (toReview no aplica y va a 0). */
    public Uni<List<EpCount>> countPerEp() {
        return getSession().chain(session -> session.createSelectionQuery("""
                        select new com.alejandro.mtoexplorer.repository.projection.EpCount(u.ep, count(u), 0L)
                        from UnrecognizedValue u where u.ep is not null group by u.ep""", EpCount.class)
                .getResultList());
    }
}
