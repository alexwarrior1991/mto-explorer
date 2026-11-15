package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Hoja DISCONNECTORS de profile-master.xlsx: un seccionador. Único por (paquete, nombre).
 * Todas sus relaciones salvo el paquete son opcionales: puede no tener estación ("SIN ESTACION"),
 * ni poste (los de pórtico de subestación), ni vía conectada.
 */
@Entity
@Table(name = "disconnector")
public class Disconnector extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public ExecutionPackage executionPackage;
    /** Null si en el maestro es "SIN ESTACION". */
    @ManyToOne(fetch = FetchType.LAZY)
    public Station station;
    /** La vía que maniobra; si el maestro no la dice, la de su poste. */
    @ManyToOne(fetch = FetchType.LAZY)
    public Track track;
    /** La otra vía, en los que ponen dos en paralelo (Disc/PP, LoadB/PP). */
    @ManyToOne(fetch = FetchType.LAZY)
    public Track connectedTrack;
    /** El poste (perfil) del que cuelga; null si no está en un poste. */
    @ManyToOne(fetch = FetchType.LAZY)
    public Profile pole;
    public String name;
    /** Solo sin poste: el KP del propio seccionador. Con poste, el KP es el del poste. */
    public Double kp;
    public boolean onLoad;
    /** true = normalmente abierto, false = cerrado, null = sin dato. */
    public Boolean normallyOpen;
    public String driveType;
    /** Código del catálogo DisconnectorFunction. */
    public String function;
    public boolean enabled;
}
