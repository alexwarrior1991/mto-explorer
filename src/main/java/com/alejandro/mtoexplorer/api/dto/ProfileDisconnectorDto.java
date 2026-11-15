package com.alejandro.mtoexplorer.api.dto;

/** Un seccionador colgado de un poste, en la ficha del perfil. */
public record ProfileDisconnectorDto(String name, String function, boolean onLoad, Boolean normallyOpen,
                                     boolean enabled) {
}
