package com.alejandro.mtoexplorer.service;

import com.alejandro.mtoexplorer.entity.Lov;
import com.alejandro.mtoexplorer.repository.LovRepository;
import com.alejandro.mtoexplorer.repository.criteria.LovSearch;
import com.alejandro.mtoexplorer.repository.projection.LovFamilyCount;
import com.alejandro.mtoexplorer.service.cache.CacheNames;
import com.alejandro.mtoexplorer.service.model.Page;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/** El catálogo de LOVs. */
@ApplicationScoped
public class LovService {

    private final LovRepository lovs;

    LovService(LovRepository lovs) {
        this.lovs = lovs;
    }

    @WithSession
    public Uni<Page<Lov>> search(LovSearch search, int page, int size) {
        PanacheQuery<Lov> query = lovs.search(search);
        return query.page(page, size).list()
                .chain(items -> query.count().map(total -> new Page<>(items, page, size, total)));
    }

    /** Una agregación de todo el catálogo que solo cambia al importar el maestro de LOVs: cacheada. */
    @CacheResult(cacheName = CacheNames.LOV_FAMILIES)
    @WithSession
    public Uni<List<LovFamilyCount>> families() {
        return lovs.countPerFamily();
    }
}