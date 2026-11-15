package com.alejandro.mtoexplorer.api.dto;

public record CantileverDto(int slot, String type, Double stagger, Double catenaryHeight, Double cwElevation,
                            Double cwHeight, Double windDeflection, Double armAngle, String steadyArmType,
                            Double steadyArmLength, boolean enabled, boolean toReview) {
}
