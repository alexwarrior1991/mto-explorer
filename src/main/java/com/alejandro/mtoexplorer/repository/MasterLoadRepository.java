package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.MasterLoad;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class MasterLoadRepository implements PanacheRepository<MasterLoad> {

    /** La última carga de ese fichero, o null si no se ha cargado nunca. */
    public Uni<MasterLoad> latest(String fileName) {
        return find("fileName", Sort.descending("loadedAt"), fileName).firstResult();
    }

    public Uni<List<MasterLoad>> recent(int max) {
        return findAll(Sort.descending("loadedAt")).page(0, max).list();
    }
}
