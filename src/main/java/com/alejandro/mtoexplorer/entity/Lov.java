package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Hoja LOVS de lov-master.xlsx: un código del catálogo. */
@Entity
@Table(name = "lov")
public class Lov extends PanacheEntity {

    /** La familia del código: Sectioning, PoleType… */
    public String entity;
    public String code;
    public String descriptionEn;
    public String descriptionEs;
    public String type;
    public Integer planNumber;
    public boolean enabled;
    public String origin;
    public String boqCategories;
    /** Paquetes en los que aparece, tal cual vienen: "EP11A, EP4, RUBI". */
    public String eps;
    public Integer usesInTracks;
    public boolean toReview;
}
