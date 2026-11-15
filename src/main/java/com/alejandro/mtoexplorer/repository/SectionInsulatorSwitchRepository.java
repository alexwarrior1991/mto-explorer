package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/** Las agujas se leen con su aislador (SectionInsulator.switches); aquí solo hace falta borrarlas al importar. */
@ApplicationScoped
public class SectionInsulatorSwitchRepository implements PanacheRepository<SectionInsulatorSwitch> {
}
