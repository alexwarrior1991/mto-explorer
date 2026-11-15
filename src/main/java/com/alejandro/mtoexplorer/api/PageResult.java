package com.alejandro.mtoexplorer.api;

import java.util.List;

/** Una página de resultados. page empieza en 0. */
public record PageResult<T>(List<T> items, int page, int size, long total) {
}
