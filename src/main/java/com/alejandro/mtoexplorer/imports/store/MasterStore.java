package com.alejandro.mtoexplorer.imports.store;

import com.alejandro.mtoexplorer.entity.MasterLoad;
import com.alejandro.mtoexplorer.imports.reader.LovMaster;
import com.alejandro.mtoexplorer.imports.reader.LovMasterReader;
import com.alejandro.mtoexplorer.imports.reader.ProfileMaster;
import com.alejandro.mtoexplorer.imports.reader.ProfileMasterReader;
import com.alejandro.mtoexplorer.repository.CantileverRepository;
import com.alejandro.mtoexplorer.repository.DisconnectorRepository;
import com.alejandro.mtoexplorer.repository.ExecutionPackageRepository;
import com.alejandro.mtoexplorer.repository.LovRepository;
import com.alejandro.mtoexplorer.repository.MasterLoadRepository;
import com.alejandro.mtoexplorer.repository.ProfileRepository;
import com.alejandro.mtoexplorer.repository.SectionInsulatorRepository;
import com.alejandro.mtoexplorer.repository.SectionInsulatorSwitchRepository;
import com.alejandro.mtoexplorer.repository.StationRepository;
import com.alejandro.mtoexplorer.repository.TrackRepository;
import com.alejandro.mtoexplorer.repository.UnrecognizedValueRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.function.Supplier;

/**
 * Guarda un maestro leído: en una transacción, borra lo que había de ese maestro, escribe lo
 * nuevo y deja el registro de la carga. Si algo falla, no se queda nada a medias.
 */
@ApplicationScoped
public class MasterStore {

    private final LovRepository lovs;
    private final UnrecognizedValueRepository unrecognized;
    private final ExecutionPackageRepository executionPackages;
    private final StationRepository stations;
    private final TrackRepository tracks;
    private final ProfileRepository profiles;
    private final CantileverRepository cantilevers;
    private final DisconnectorRepository disconnectors;
    private final SectionInsulatorRepository insulators;
    private final SectionInsulatorSwitchRepository switches;
    private final MasterLoadRepository loads;

    MasterStore(LovRepository lovs, UnrecognizedValueRepository unrecognized,
                ExecutionPackageRepository executionPackages, StationRepository stations, TrackRepository tracks,
                ProfileRepository profiles, CantileverRepository cantilevers, DisconnectorRepository disconnectors,
                SectionInsulatorRepository insulators, SectionInsulatorSwitchRepository switches,
                MasterLoadRepository loads) {
        this.lovs = lovs;
        this.unrecognized = unrecognized;
        this.executionPackages = executionPackages;
        this.stations = stations;
        this.tracks = tracks;
        this.profiles = profiles;
        this.cantilevers = cantilevers;
        this.disconnectors = disconnectors;
        this.insulators = insulators;
        this.switches = switches;
        this.loads = loads;
    }

    /** La última carga de ese fichero, o null si no se ha cargado nunca. */
    @WithSession
    public Uni<MasterLoad> latestLoad(String fileName) {
        return loads.latest(fileName);
    }

    /**
     * Sustituye el maestro de LOVs. {@code load} se pide al final, ya escritos los datos, para que
     * su duración cuente también la escritura.
     */
    @WithTransaction
    public Uni<MasterLoad> replace(LovMaster master, Supplier<MasterLoad> load) {
        return lovs.deleteAll()
                .chain(() -> unrecognized.deleteByMaster(LovMasterReader.MASTER))
                .chain(() -> persistAll(master.lovs()))
                .chain(() -> persistAll(master.unrecognized()))
                .chain(() -> loads.persist(load.get()));
    }

    /** Sustituye el maestro de infraestructura. Se borra de hijos a padres y se escribe de padres a hijos. */
    @WithTransaction
    public Uni<MasterLoad> replace(ProfileMaster master, Supplier<MasterLoad> load) {
        return switches.deleteAll()
                .chain(() -> insulators.deleteAll())
                .chain(() -> disconnectors.deleteAll())
                .chain(() -> cantilevers.deleteAll())
                .chain(() -> profiles.deleteAll())
                .chain(() -> tracks.deleteAll())
                .chain(() -> stations.deleteAll())
                .chain(() -> executionPackages.deleteAll())
                .chain(() -> unrecognized.deleteByMaster(ProfileMasterReader.MASTER))
                .chain(() -> persistAll(master.executionPackages()))
                .chain(() -> persistAll(master.stations()))
                .chain(() -> persistAll(master.tracks()))
                .chain(() -> persistAll(master.profiles()))
                .chain(() -> persistAll(master.cantilevers()))
                .chain(() -> persistAll(master.disconnectors()))
                .chain(() -> persistAll(master.sectionInsulators()))
                .chain(() -> persistAll(master.switches()))
                .chain(() -> persistAll(master.unrecognized()))
                .chain(() -> loads.persist(load.get()));
    }

    /**
     * Persiste una lista entera de una vez. Panache falla con una lista vacía ("The Uni set is
     * empty"), y en el maestro real la hoja NO_RECONOCIDO está vacía.
     */
    private static Uni<Void> persistAll(List<?> entities) {
        if (entities.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        return Panache.getSession().chain(session -> session.persistAll(entities.toArray()));
    }
}
