package com.alejandro.mtoexplorer.api.dto;

/** Un paquete con cuántas vías y perfiles tiene. */
public record EpOverviewDto(String code, String name, boolean enabled, long tracks, long profiles) {
}
