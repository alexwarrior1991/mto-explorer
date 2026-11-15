package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;

/** Hoja CANTILEVERS de profile-master.xlsx: una ménsula de un perfil (un perfil puede tener varias). */
@Entity
@Table(name = "cantilever")
public class Cantilever extends PanacheEntity {

    public String ep;
    public String track;
    public String profileId;
    /** El ORDEN del perfil al que pertenece. */
    public int profileOrder;
    public int slot;
    public String type;
    public Double stagger;
    public Double catenaryHeight;
    public Double cwElevation;
    public Double cwHeight;
    public Double windDeflection;
    public Double armAngle;
    public String steadyArmType;
    public Double steadyArmLength;
    public boolean enabled;
    public boolean toReview;
    public Integer sourceRow;

    public static Uni<List<Cantilever>> listByProfile(String ep, String track, int profileOrder) {
        return list("ep = ?1 and track = ?2 and profileOrder = ?3", Sort.by("slot"), ep, track, profileOrder);
    }
}
