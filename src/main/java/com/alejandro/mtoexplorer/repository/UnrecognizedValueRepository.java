package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.UnrecognizedValue;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class UnrecognizedValueRepository implements PanacheRepository<UnrecognizedValue> {

    /** Los valores de un maestro (LOV o PROFILE) y de un paquete; los dos filtros son opcionales. */
    public Uni<List<UnrecognizedValue>> search(String master, String ep) {
        Sort sort = Sort.by("master").and("ep").and("kind").and("value");
        List<String> where = new ArrayList<>();
        Parameters params = new Parameters();
        if (master != null) {
            where.add("master = :master");
            params.and("master", master);
        }
        if (ep != null) {
            where.add("ep = :ep");
            params.and("ep", ep);
        }
        return where.isEmpty() ? listAll(sort) : list(String.join(" and ", where), sort, params);
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
