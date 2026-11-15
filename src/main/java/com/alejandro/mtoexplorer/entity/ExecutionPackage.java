package com.alejandro.mtoexplorer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Hoja EPS de profile-master.xlsx. */
@Entity
@Table(name = "execution_package")
public class ExecutionPackage extends BaseEntity {

    public String code;
    public String name;
    public boolean initialPackage;
    @Column(name = "length_m")
    public Integer lengthMeters;
    public LocalDate startDate;
    public LocalDate endDate;
    public String companyId;
    public boolean enabled;
}
