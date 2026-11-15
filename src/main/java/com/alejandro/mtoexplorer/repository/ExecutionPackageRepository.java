package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ExecutionPackageRepository implements PanacheRepository<ExecutionPackage> {

    public Uni<List<ExecutionPackage>> listAllByCode() {
        return listAll(Sort.by("code"));
    }

    /** El paquete con ese código, o null si no existe. */
    public Uni<ExecutionPackage> findByCode(String code) {
        return find("code", code).firstResult();
    }
}
