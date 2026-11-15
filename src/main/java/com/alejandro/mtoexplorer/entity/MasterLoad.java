package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Una carga de un fichero maestro. La última de cada fichero dice si el Excel ha cambiado desde entonces.
 */
@Entity
@Table(name = "master_load")
public class MasterLoad extends BaseEntity {

    public String fileName;
    public Instant lastModified;
    public long sizeBytes;
    public Instant loadedAt;
    public int rowCount;
    public long durationMs;
}
