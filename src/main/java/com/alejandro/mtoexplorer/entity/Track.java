package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** Hoja TRACKS de profile-master.xlsx. Única por (paquete, nombre). */
@Entity
@Table(name = "track")
public class Track extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public ExecutionPackage executionPackage;
    public String name;

    /**
     * Las estaciones que recorre, en el orden del maestro ("MOM | PMO"). Una vía pasa por varias
     * estaciones y una estación tiene varias vías: {@code @ManyToMany}, con la tabla intermedia
     * track_station. {@code @OrderColumn} guarda la posición de cada una, para no perder el orden.
     */
    @ManyToMany
    @JoinTable(name = "track_station",
            joinColumns = @JoinColumn(name = "track_id"),
            inverseJoinColumns = @JoinColumn(name = "station_id"))
    @OrderColumn(name = "station_order")
    public List<Station> stations = new ArrayList<>();

    public boolean enabled;
    public String sourceSheet;
}
