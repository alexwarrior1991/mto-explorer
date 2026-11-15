package com.alejandro.mtoexplorer.service.model;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados. {@code page} empieza en 0. */
public record Page<T>(List<T> items, int page, int size, long total) {

    /** La misma página con cada elemento transformado (por ejemplo, de entidad a DTO). */
    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), page, size, total);
    }
}
