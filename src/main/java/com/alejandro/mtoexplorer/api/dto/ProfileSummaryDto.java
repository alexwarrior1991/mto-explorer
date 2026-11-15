package com.alejandro.mtoexplorer.api.dto;

/** Un perfil en un listado. {@code kpText} es el PK como en los planos: 17+858.5. */
public record ProfileSummaryDto(String ep, String track, int order, String profileId, Double kp, String kpText,
                                String status, String sectioning, String poleType, String foundation,
                                Double span, boolean toReview) {
}
