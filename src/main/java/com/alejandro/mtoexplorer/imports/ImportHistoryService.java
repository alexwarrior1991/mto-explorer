package com.alejandro.mtoexplorer.imports;

import com.alejandro.mtoexplorer.entity.MasterLoad;
import com.alejandro.mtoexplorer.repository.MasterLoadRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/** El historial de cargas. Aparte de {@link MasterImportService}, que es bloqueante: este es reactivo. */
@ApplicationScoped
public class ImportHistoryService {

    private final MasterLoadRepository loads;

    ImportHistoryService(MasterLoadRepository loads) {
        this.loads = loads;
    }

    @WithSession
    public Uni<List<MasterLoad>> recent(int max) {
        return loads.recent(max);
    }
}
