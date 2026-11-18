package com.alejandro.mtoexplorer.service.model;

import com.alejandro.mtoexplorer.entity.ExecutionPackage;

/**
 * Un paquete con cuántas vías y perfiles tiene. Datos planos, sin la entidad: se cachea en Redis
 * como JSON (ver {@code CacheNames}).
 */
public record EpOverview(String code, String name, boolean enabled, long tracks, long profiles) {
}
