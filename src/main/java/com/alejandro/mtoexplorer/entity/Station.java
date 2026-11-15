package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;

/** Hoja STATIONS de profile-master.xlsx. */
@Entity
@Table(name = "station")
public class Station extends PanacheEntity {

    public String ep;
    public String name;

    public static Uni<List<Station>> listByEp(String ep) {
        return list("ep", Sort.by("name"), ep);
    }
}
