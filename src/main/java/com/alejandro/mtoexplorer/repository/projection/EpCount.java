package com.alejandro.mtoexplorer.repository.projection;

/** Un recuento por paquete: cuántas filas tiene y cuántas están marcadas REVISAR. */
public record EpCount(String ep, long total, long toReview) {
}
