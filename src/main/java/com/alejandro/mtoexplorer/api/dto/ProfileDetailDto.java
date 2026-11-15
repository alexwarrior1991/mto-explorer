package com.alejandro.mtoexplorer.api.dto;

import java.util.List;

/** La ficha de un perfil, con sus ménsulas. */
public record ProfileDetailDto(String ep, String track, int order, String profileId, Double kp, String kpText,
                               String status, String sectioning, String anchorage, String anchorageFoundation,
                               String foundation, String poleType, String portal, String returnSupport,
                               String sectioningFeeding, String supportType, String assemblyConfiguration,
                               Double span, Double heightCantileverSupport, Double poleGaugeLocation,
                               Double railPoleDistance, boolean enabled, boolean toReview,
                               String sourceSheet, Integer sourceRow, List<CantileverDto> cantilevers) {
}
