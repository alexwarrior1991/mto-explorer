package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Hoja STATIONS de profile-master.xlsx. Única por (paquete, nombre). */
@Entity
@Table(name = "station")
public class Station extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public ExecutionPackage executionPackage;
    public String name;
}
