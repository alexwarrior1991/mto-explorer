package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Hoja CANTILEVERS de profile-master.xlsx: una ménsula de un perfil (un perfil puede tener varias). */
@Entity
@Table(name = "cantilever")
public class Cantilever extends BaseEntity {

    /**
     * El perfil del que cuelga. LAZY: no se carga al leer la ménsula. En Hibernate Reactive eso
     * significa que acceder a {@code profile.kp} sin haberlo cargado antes falla; hay que pedirlo
     * (un {@code join fetch} en la consulta o {@code Mutiny.fetch}).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Profile profile;
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
