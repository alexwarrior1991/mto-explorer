package com.alejandro.mtoexplorer.api.dto;

/** Una familia del catálogo: cuántos códigos tiene y cuántos quedan por revisar. */
public record LovFamilyDto(String entity, long codes, long toReview) {
}
