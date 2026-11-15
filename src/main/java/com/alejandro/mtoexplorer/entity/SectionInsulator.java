package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Hoja SECTION_INSULATORS de profile-master.xlsx: un aislador de sección. Clave natural: (ep, station, name). */
@Entity
@Table(name = "section_insulator")
public class SectionInsulator extends PanacheEntity {

    public String ep;
    public String station;
    public String name;
    public Double kp;
    /** TRACK_CONNECTION (separa dos vías unidas por una aguja) o IN_TRACK (en medio de una vía). */
    public String installationType;
    public String track;
    /** Vacía en los IN_TRACK. */
    public String connectedTrack;
    public boolean enabled;
}
