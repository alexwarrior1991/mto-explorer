package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.Dtos.EpQuality;
import com.alejandro.mtoexplorer.api.Dtos.UnrecognizedDto;
import com.alejandro.mtoexplorer.entity.UnrecognizedValue;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Lo que queda por revisar (REVISAR = SI) o por mapear (NO_RECONOCIDO), por paquete. */
@Path("/api/quality")
@WithSession
public class QualityResource {

    private static final String PROFILES =
            "select p.ep, count(p), sum(case when p.toReview then 1 else 0 end) from Profile p group by p.ep";
    private static final String CANTILEVERS =
            "select c.ep, count(c), sum(case when c.toReview then 1 else 0 end) from Cantilever c group by c.ep";
    private static final String UNRECOGNIZED =
            "select u.ep, count(u), 0 from UnrecognizedValue u where u.ep is not null group by u.ep";

    @GET
    @Operation(summary = "Por paquete: perfiles y ménsulas, cuántos quedan por revisar y cuántos valores sin reconocer")
    public Uni<List<EpQuality>> byExecutionPackage() {
        // Por paquete, un array: [perfiles, perfiles por revisar, ménsulas, ménsulas por revisar, no reconocidos].
        Map<String, long[]> byEp = new TreeMap<>();
        return Panache.getSession().chain(session ->
                        count(session, PROFILES, byEp, 0)
                                .chain(() -> count(session, CANTILEVERS, byEp, 2))
                                .chain(() -> count(session, UNRECOGNIZED, byEp, 4)))
                .map(ignored -> byEp.entrySet().stream()
                        .map(e -> new EpQuality(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2],
                                e.getValue()[3], e.getValue()[4]))
                        .toList());
    }

    @GET
    @Path("/unrecognized")
    @Operation(summary = "Valores de la hoja NO_RECONOCIDO, filtrables por maestro (LOV o PROFILE) y paquete")
    public Uni<List<UnrecognizedDto>> unrecognized(@QueryParam("master") String master, @QueryParam("ep") String ep) {
        List<String> where = new ArrayList<>();
        Parameters params = new Parameters();
        if (master != null && !master.isBlank()) {
            where.add("master = :master");
            params.and("master", master.toUpperCase());
        }
        if (ep != null && !ep.isBlank()) {
            where.add("ep = :ep");
            params.and("ep", ep);
        }
        Sort sort = Sort.by("master").and("ep").and("kind").and("value");
        Uni<List<UnrecognizedValue>> values = where.isEmpty()
                ? UnrecognizedValue.listAll(sort)
                : UnrecognizedValue.list(String.join(" and ", where), sort, params);
        return values.map(list -> list.stream().map(UnrecognizedDto::of).toList());
    }

    /** Ejecuta una consulta "ep, total, por revisar" y la vuelca en byEp a partir de la posición offset. */
    private static Uni<Void> count(Mutiny.Session session, String hql, Map<String, long[]> byEp, int offset) {
        return session.createSelectionQuery(hql, Object[].class).getResultList()
                .invoke(rows -> rows.forEach(row -> {
                    long[] counts = byEp.computeIfAbsent((String) row[0], k -> new long[5]);
                    counts[offset] = ((Number) row[1]).longValue();
                    if (offset + 1 < counts.length) {
                        counts[offset + 1] = ((Number) row[2]).longValue();
                    }
                }))
                .replaceWithVoid();
    }
}
