package com.alejandro.mtoexplorer.service.model;

import com.alejandro.mtoexplorer.entity.ExecutionPackage;

/** Un paquete con cuántas vías y perfiles tiene. */
public record EpOverview(ExecutionPackage executionPackage, long tracks, long profiles) {
}
