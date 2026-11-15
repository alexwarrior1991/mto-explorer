package com.alejandro.mtoexplorer.service;

import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;
import com.alejandro.mtoexplorer.repository.DisconnectorRepository;
import com.alejandro.mtoexplorer.repository.SectionInsulatorRepository;
import com.alejandro.mtoexplorer.repository.SectionInsulatorSwitchRepository;
import com.alejandro.mtoexplorer.service.model.InsulatorWithSwitches;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** El seccionamiento de un paquete: seccionadores y aisladores de sección con sus agujas. */
@ApplicationScoped
public class SectioningService {

    private final DisconnectorRepository disconnectors;
    private final SectionInsulatorRepository insulators;
    private final SectionInsulatorSwitchRepository switches;

    SectioningService(DisconnectorRepository disconnectors, SectionInsulatorRepository insulators,
                      SectionInsulatorSwitchRepository switches) {
        this.disconnectors = disconnectors;
        this.insulators = insulators;
        this.switches = switches;
    }

    @WithSession
    public Uni<List<Disconnector>> disconnectors(String ep, String station, String track) {
        return disconnectors.search(ep, station, track);
    }

    /**
     * Los aisladores con sus agujas: dos consultas seguidas (los aisladores y, después, todas las
     * agujas del paquete) y se reparten en memoria. Un aislador es (ep, estación, nombre): el mismo
     * nombre se repite en estaciones distintas.
     */
    @WithSession
    public Uni<List<InsulatorWithSwitches>> sectionInsulators(String ep, String station) {
        return insulators.search(ep, station)
                .chain(found -> switches.search(ep, station).map(allSwitches -> {
                    Map<String, List<SectionInsulatorSwitch>> byInsulator = allSwitches.stream()
                            .collect(Collectors.groupingBy(w -> key(w.station, w.insulator)));
                    return found.stream()
                            .map(i -> new InsulatorWithSwitches(i,
                                    byInsulator.getOrDefault(key(i.station, i.name), List.of())))
                            .toList();
                }));
    }

    private static String key(String station, String insulator) {
        return station + "|" + insulator;
    }
}
