package com.alejandro.mtoexplorer.repository.criteria;

/** Filtros del catálogo de LOVs: familia y texto (en el código o en las descripciones). Ambos opcionales. */
public record LovSearch(String entity, String text) {
}
