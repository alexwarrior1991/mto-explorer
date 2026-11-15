package com.alejandro.mtoexplorer.api.dto;

import java.time.Instant;

public record MasterLoadDto(String file, Instant lastModified, long sizeBytes, Instant loadedAt,
                            int rows, long durationMs) {
}
