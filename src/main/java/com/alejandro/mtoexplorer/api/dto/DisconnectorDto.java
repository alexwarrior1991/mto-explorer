package com.alejandro.mtoexplorer.api.dto;

/**
 * Un seccionador. Con poste: {@code profileId}, {@code poleTrack} y {@code poleOrder} dicen cuál
 * es (con ellos se pide su ficha: /api/profiles/{ep}/{poleTrack}/{poleOrder}) y {@code poleKp} es
 * el KP del poste. Sin poste, {@code kp} es el del propio seccionador.
 */
public record DisconnectorDto(String ep, String station, String name, String track, String connectedTrack,
                              String profileId, String poleTrack, Integer poleOrder, Double poleKp,
                              String poleKpText, Double kp, String kpText, boolean onLoad, Boolean normallyOpen,
                              String driveType, String function, boolean enabled) {
}
