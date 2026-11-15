package com.alejandro.mtoexplorer.api.dto;

public record DisconnectorDto(String ep, String station, String name, String track, String connectedTrack,
                              String profileId, Double poleKp, String poleKpText, Double kp, String kpText,
                              boolean onLoad, Boolean normallyOpen, String driveType, String function,
                              boolean enabled) {
}
