package com.alejandro.mtoexplorer.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.List;

/** Hoja EPS de profile-master.xlsx. */
@Entity
@Table(name = "execution_package")
public class ExecutionPackage extends PanacheEntity {

    public String code;
    public String name;
    public boolean initialPackage;
    @Column(name = "length_m")
    public Integer lengthMeters;
    public LocalDate startDate;
    public LocalDate endDate;
    public String companyId;
    public boolean enabled;

    public static Uni<List<ExecutionPackage>> listAllByCode() {
        return listAll(Sort.by("code"));
    }

    /** El paquete con ese código, o null si no existe. */
    public static Uni<ExecutionPackage> findByCode(String code) {
        return find("code", code).firstResult();
    }
}
