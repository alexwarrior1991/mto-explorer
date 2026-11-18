package com.alejandro.mtoexplorer.service.model;

/**
 * Un paquete con cuántas vías y perfiles tiene. Datos planos, sin la entidad: se cachea en Redis
 * como JSON (ver {@code CacheNames}).
 */
public record EpOverview(String code, String name, boolean enabled, long tracks, long profiles) {
}
