package com.alejandro.mtoexplorer.service.model;

/** Calidad de un paquete: cuánto queda por revisar (REVISAR = SI) o por mapear (NO_RECONOCIDO). */
public record EpQuality(String ep, long profiles, long profilesToReview, long cantilevers,
                        long cantileversToReview, long unrecognized) {
}
