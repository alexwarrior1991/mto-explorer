package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;

/** Hoja TRACKS de profile-master.xlsx. */
@Entity
@Table(name = "track")
public class Track extends PanacheEntity {

    public String ep;
    public String name;
    /** Estaciones que recorre, tal cual vienen: "MOM | PMO". */
    public String stations;
    public boolean enabled;
    public String sourceSheet;

    public static Uni<List<Track>> listByEp(String ep) {
        return list("ep", Sort.by("name"), ep);
    }
}
