package com.alejandro.mtoexplorer.repository.projection;

/** Una familia del catálogo de LOVs: cuántos códigos tiene y cuántos están marcados REVISAR. */
public record LovFamilyCount(String entity, long codes, long toReview) {
}
