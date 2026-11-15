package com.alejandro.mtoexplorer.api.dto;

/** Calidad de un paquete: cuánto queda por revisar o por mapear. */
public record EpQualityDto(String ep, long profiles, long profilesToReview, long cantilevers,
                           long cantileversToReview, long unrecognized) {
}
