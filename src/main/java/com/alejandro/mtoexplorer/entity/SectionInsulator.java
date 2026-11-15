package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** Hoja SECTION_INSULATORS de profile-master.xlsx: un aislador de sección. Único por (estación, nombre). */
@Entity
@Table(name = "section_insulator")
public class SectionInsulator extends BaseEntity {

    /** Su estación; el paquete es el de la estación. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Station station;
    public String name;
    public Double kp;
    /** TRACK_CONNECTION (separa dos vías unidas por una aguja) o IN_TRACK (en medio de una vía). */
    public String installationType;
    @ManyToOne(fetch = FetchType.LAZY)
    public Track track;
    /** Null en los IN_TRACK. */
    @ManyToOne(fetch = FetchType.LAZY)
    public Track connectedTrack;
    public boolean enabled;

    @OneToMany(mappedBy = "insulator")
    @OrderBy("kp, code")
    public List<SectionInsulatorSwitch> switches = new ArrayList<>();
}
