package com.alejandro.mtoexplorer.repository.criteria;

/**
 * Filtros de la búsqueda de perfiles. Solo {@code ep} es obligatorio; el PK ya va en metros
 * (la API se encarga de entender "12+345").
 */
public record ProfileSearch(String ep, String track, Double kpFrom, Double kpTo, Boolean toReview) {
}
