package com.alejandro.mtoexplorer.api.dto;

import java.util.List;

/** Un código del catálogo, con los paquetes en los que aparece como lista. */
public record LovDto(String entity, String code, String descriptionEn, String descriptionEs, String type,
                     Integer planNumber, boolean enabled, String origin, List<String> eps,
                     Integer usesInTracks, boolean toReview) {
}
