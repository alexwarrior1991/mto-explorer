package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Hoja STATIONS de profile-master.xlsx. */
@Entity
@Table(name = "station")
public class Station extends BaseEntity {

    public String ep;
    public String name;
}
