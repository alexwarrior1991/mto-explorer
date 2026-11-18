package com.alejandro.mtoexplorer.api.dto;

import java.time.Instant;

/** Un aviso del canal SSE de importaciones: qué maestro, qué fichero, qué pasó y cuándo. */
public record ImportEventDto(Instant at, String master, String file, String status, int rows, long durationMs,
                             String detail) {
}
