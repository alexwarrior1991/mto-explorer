package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Hoja PROFILES de profile-master.xlsx: un poste o pórtico de una vía.
 * PROFILE_ID se repite dentro de una vía; lo que identifica el perfil es (vía, sortOrder).
 */
@Entity
@Table(name = "profile")
public class Profile extends BaseEntity {

    /** Su vía; el paquete es el de la vía ({@code track.executionPackage}). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Track track;
    public String profileId;
    /**
     * Punto kilométrico en metros: 17858.5 es el 17+858.5. Puede ser negativo (antes del origen).
     */
    public Double kp;
    @Column(name = "sort_order")
    public int sortOrder;
    public String status;
    public String sectioning;
    public String anchorage;
    public String anchorageFoundation;
    public String foundation;
    public String poleType;
    public String portal;
    public String returnSupport;
    public String sectioningFeeding;
    public String supportType;
    public String assemblyConfiguration;
    public Double span;
    public Double heightCantileverSupport;
    public Double poleGaugeLocation;
    public Double railPoleDistance;
    public boolean enabled;
    public boolean toReview;
    public String sourceSheet;
    public Integer sourceRow;

    /**
     * Sus ménsulas, por posición. {@code mappedBy}: la clave foránea es de la ménsula
     * ({@link Cantilever#profile}); esta lista es el otro lado de la misma relación y no escribe nada.
     * Las colecciones son LAZY por defecto: se cargan con {@code Mutiny.fetch(profile.cantilevers)}.
     */
    @OneToMany(mappedBy = "profile")
    @OrderBy("slot")
    public List<Cantilever> cantilevers = new ArrayList<>();

    /** Los seccionadores que cuelgan de este poste (casi siempre ninguno o uno). */
    @OneToMany(mappedBy = "pole")
    @OrderBy("name")
    public List<Disconnector> disconnectors = new ArrayList<>();
}
