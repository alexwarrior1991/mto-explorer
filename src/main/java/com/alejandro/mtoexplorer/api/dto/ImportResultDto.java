package com.alejandro.mtoexplorer.api.dto;

/** Lo que pasó con un fichero al importar: LOADED, UNCHANGED o MISSING. */
public record ImportResultDto(String file, String status, int rows, long durationMs, String detail) {
}
