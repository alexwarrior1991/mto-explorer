package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.Dtos.LovDto;
import com.alejandro.mtoexplorer.api.Dtos.LovEntityCount;
import com.alejandro.mtoexplorer.entity.Lov;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.ArrayList;
import java.util.List;

@Path("/api/lovs")
@WithSession
public class LovResource {

    @GET
    @Operation(summary = "Códigos del catálogo, filtrables por familia y por texto (código o descripción)")
    public Uni<PageResult<LovDto>> search(@QueryParam("entity") String entity,
                                          @QueryParam("q") String text,
                                          @QueryParam("page") @DefaultValue("0") @Min(0) int page,
                                          @QueryParam("size") @DefaultValue("100") @Min(1) @Max(500) int size) {
        List<String> where = new ArrayList<>();
        Parameters params = new Parameters();
        if (entity != null && !entity.isBlank()) {
            where.add("entity = :entity");
            params.and("entity", entity);
        }
        if (text != null && !text.isBlank()) {
            where.add("(lower(code) like :text or lower(descriptionEn) like :text or lower(descriptionEs) like :text)");
            params.and("text", "%" + text.toLowerCase() + "%");
        }
        Sort sort = Sort.by("entity").and("code");
        PanacheQuery<Lov> query = where.isEmpty()
                ? Lov.findAll(sort)
                : Lov.find(String.join(" and ", where), sort, params);
        return query.page(page, size).list()
                .chain(lovs -> query.count().map(total -> new PageResult<>(
                        lovs.stream().map(LovDto::of).toList(), page, size, total)));
    }

    @GET
    @Path("/entities")
    @Operation(summary = "Familias del catálogo con cuántos códigos tienen y cuántos quedan por revisar")
    public Uni<List<LovEntityCount>> entities() {
        return Panache.getSession().chain(session -> session.createSelectionQuery("""
                        select new com.alejandro.mtoexplorer.api.Dtos$LovEntityCount(
                            l.entity, count(l), sum(case when l.toReview then 1 else 0 end))
                        from Lov l group by l.entity order by l.entity""", LovEntityCount.class)
                .getResultList());
    }
}
