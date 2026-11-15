package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Hoja SECTION_INSULATOR_SWITCHES de profile-master.xlsx: una aguja de un aislador. Única por (aislador, código). */
@Entity
@Table(name = "section_insulator_switch")
public class SectionInsulatorSwitch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public SectionInsulator insulator;
    /** El del plano: W31, W110… */
    public String code;
    public Double kp;
    /** Solo el denominador: 9 es una aguja 1:9. */
    public Integer tangent;
    @ManyToOne(fetch = FetchType.LAZY)
    public Track track;
    /** Al contrario que en el resto de hojas, NO no la quita: la aguja está fuera de servicio. */
    public boolean enabled;
}
