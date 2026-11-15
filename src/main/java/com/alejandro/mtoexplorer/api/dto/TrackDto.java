package com.alejandro.mtoexplorer.api.dto;

import java.util.List;

/** Una vía, con sus estaciones como lista (en el maestro vienen como "MOM | PMO"). */
public record TrackDto(String ep, String name, List<String> stations, boolean enabled, String sourceSheet) {
}
