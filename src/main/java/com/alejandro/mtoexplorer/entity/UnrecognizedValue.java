package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Hoja NO_RECONOCIDO de cualquiera de los dos maestros: lo que el generador no supo mapear. */
@Entity
@Table(name = "unrecognized_value")
public class UnrecognizedValue extends BaseEntity {

    /** LOV o PROFILE: de qué maestro sale. */
    public String master;
    public String kind;
    public String value;
    public String ep;
    public String sheet;
    public Integer firstRow;
    public Integer occurrences;
}
