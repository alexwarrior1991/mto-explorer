package com.alejandro.mtoexplorer.service;

import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.SectionInsulator;
import com.alejandro.mtoexplorer.repository.DisconnectorRepository;
import com.alejandro.mtoexplorer.repository.SectionInsulatorRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/** El seccionamiento de un paquete: seccionadores y aisladores de sección con sus agujas. */
@ApplicationScoped
public class SectioningService {

    private final DisconnectorRepository disconnectors;
    private final SectionInsulatorRepository insulators;

    SectioningService(DisconnectorRepository disconnectors, SectionInsulatorRepository insulators) {
        this.disconnectors = disconnectors;
        this.insulators = insulators;
    }

    /** {@code station} puede ser "SIN ESTACION", como en el maestro. */
    @WithSession
    public Uni<List<Disconnector>> disconnectors(String ep, String station, String track) {
        return disconnectors.search(ep, station, track);
    }

    /** Los aisladores con sus agujas ya cargadas ({@code insulator.switches}). */
    @WithSession
    public Uni<List<SectionInsulator>> sectionInsulators(String ep, String station) {
        return insulators.search(ep, station);
    }
}
