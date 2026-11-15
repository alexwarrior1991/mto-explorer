package com.alejandro.mtoexplorer.api.dto;

import java.util.List;

public record SectionInsulatorDto(String ep, String station, String name, Double kp, String kpText,
                                  String installationType, String track, String connectedTrack,
                                  boolean enabled, List<SwitchDto> switches) {
}
