package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Hoja DISCONNECTORS de profile-master.xlsx: un seccionador. Su clave natural es (ep, name).
 * Con poste, cuelga del perfil profileId (poleKp distingue dos postes con el mismo id); sin poste,
 * lleva su propio kp.
 */
@Entity
@Table(name = "disconnector")
public class Disconnector extends PanacheEntity {

    public String ep;
    /** La estación, o "SIN ESTACION" si no es de ninguna. */
    public String station;
    /** La vía que maniobra. */
    public String track;
    /** La otra vía, en los que ponen dos en paralelo (Disc/PP, LoadB/PP). */
    public String connectedTrack;
    public String profileId;
    public Double poleKp;
    public String name;
    /** Solo sin poste: el KP del propio seccionador. */
    public Double kp;
    public boolean onLoad;
    /** true = normalmente abierto, false = cerrado, null = sin dato. */
    public Boolean normallyOpen;
    public String driveType;
    /** Código del catálogo DisconnectorFunction. */
    public String function;
    public boolean enabled;
}
