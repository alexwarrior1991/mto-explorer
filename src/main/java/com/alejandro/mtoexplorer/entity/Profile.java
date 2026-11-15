package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Hoja PROFILES de profile-master.xlsx: un poste o pórtico de una vía.
 * PROFILE_ID se repite dentro de una vía; lo que identifica el perfil es (ep, track, sortOrder).
 */
@Entity
@Table(name = "profile")
public class Profile extends PanacheEntity {

    public String ep;
    public String track;
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
     * El perfil, o null si no existe.
     */
    public static Uni<Profile> findByTrackAndOrder(String ep, String track, int sortOrder) {
        return find("ep = ?1 and track = ?2 and sortOrder = ?3", ep, track, sortOrder)
                .firstResult();
    }
}
