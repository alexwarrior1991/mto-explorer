package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Hoja CANTILEVERS de profile-master.xlsx: una ménsula de un perfil (un perfil puede tener varias). */
@Entity
@Table(name = "cantilever")
public class Cantilever extends BaseEntity {

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
}
