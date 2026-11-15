package com.alejandro.mtoexplorer.api.dto;

import java.time.LocalDate;

public record ExecutionPackageDto(String code, String name, boolean initialPackage, Integer lengthMeters,
                                  LocalDate startDate, LocalDate endDate, String companyId, boolean enabled) {
}
