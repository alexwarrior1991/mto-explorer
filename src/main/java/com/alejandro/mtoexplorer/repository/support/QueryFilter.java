package com.alejandro.mtoexplorer.repository.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * El WHERE de una consulta con filtros opcionales: cada condición entra solo si su valor no es
 * null, junto con su parámetro con nombre.
 *
 * <pre>{@code
 * QueryFilter filter = QueryFilter.create()
 *         .eq("ep", search.ep())
 *         .eq("track", search.track())              // si track es null, no filtra por vía
 *         .where("kp >= :kpFrom", "kpFrom", search.kpFrom());
 * find(filter.hql(), sort, filter.params());
 * }</pre>
 *
 * <p>Los parámetros van en un {@code Map<String, Object>}, que es lo que aceptan {@code find},
 * {@code list} y {@code count} de Panache. Sustituye a {@code io.quarkus.panache.common.Parameters},
 * obsoleta desde Quarkus 3.34.
 */
public final class QueryFilter {

    private final List<String> conditions = new ArrayList<>();
    private final Map<String, Object> params = new LinkedHashMap<>();

    private QueryFilter() {
    }

    public static QueryFilter create() {
        return new QueryFilter();
    }

    /** {@code field = :field}, si {@code value} no es null. */
    public QueryFilter eq(String field, Object value) {
        return where(field + " = :" + field, field, value);
    }

    /**
     * Una condición cualquiera con un parámetro, si {@code value} no es null. La condición puede
     * usar el parámetro varias veces: {@code (track = :track or connectedTrack = :track)}.
     */
    public QueryFilter where(String condition, String param, Object value) {
        if (value != null) {
            conditions.add(condition);
            params.put(param, value);
        }
        return this;
    }

    /** Sin ninguna condición: hay que usar findAll/listAll, porque Panache no acepta una consulta vacía. */
    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    /** Las condiciones unidas con {@code and}, en el orden en que se añadieron. */
    public String hql() {
        return String.join(" and ", conditions);
    }

    public Map<String, Object> params() {
        return Collections.unmodifiableMap(params);
    }
}
