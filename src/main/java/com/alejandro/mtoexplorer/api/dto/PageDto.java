package com.alejandro.mtoexplorer.api.dto;

import java.util.List;

/** Una página de resultados. {@code page} empieza en 0. */
public record PageDto<T>(List<T> items, int page, int size, long total) {
}
