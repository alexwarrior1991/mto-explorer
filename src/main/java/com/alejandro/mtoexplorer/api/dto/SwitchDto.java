package com.alejandro.mtoexplorer.api.dto;

/** Una aguja: "W31", con su tangente escrita como en el plano ("1:9"). */
public record SwitchDto(String code, Double kp, String kpText, Integer tangent, String tangentText,
                        String track, boolean enabled) {
}
