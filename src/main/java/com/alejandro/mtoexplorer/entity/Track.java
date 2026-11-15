package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Hoja TRACKS de profile-master.xlsx. */
@Entity
@Table(name = "track")
public class Track extends BaseEntity {

    public String ep;
    public String name;
    /** Estaciones que recorre, tal cual vienen: "MOM | PMO". */
    public String stations;
    public boolean enabled;
    public String sourceSheet;
}
