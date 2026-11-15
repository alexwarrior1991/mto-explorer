package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

/**
 * Una carga de un fichero maestro. La última de cada fichero dice si el Excel ha cambiado desde entonces.
 */
@Entity
@Table(name = "master_load")
public class MasterLoad extends PanacheEntity {

    public String fileName;
    public Instant lastModified;
    public long sizeBytes;
    public Instant loadedAt;
    public int rowCount;
    public long durationMs;

    /**
     * La última carga de ese fichero, o null si no se ha cargado nunca.
     */
    public static Uni<MasterLoad> latest(String fileName) {
        return find("filename", Sort.descending("loadedAt"), fileName).firstResult();
    }

    public static Uni<List<MasterLoad>> recent(int max) {
        return findAll(Sort.descending("loadedAt")).page(0, max).list();
    }

}
