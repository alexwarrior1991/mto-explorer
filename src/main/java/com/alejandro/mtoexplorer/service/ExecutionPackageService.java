package com.alejandro.mtoexplorer.service;

import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import com.alejandro.mtoexplorer.entity.Station;
import com.alejandro.mtoexplorer.entity.Track;
import com.alejandro.mtoexplorer.repository.ExecutionPackageRepository;
import com.alejandro.mtoexplorer.repository.ProfileRepository;
import com.alejandro.mtoexplorer.repository.StationRepository;
import com.alejandro.mtoexplorer.repository.TrackRepository;
import com.alejandro.mtoexplorer.service.cache.CacheNames;
import com.alejandro.mtoexplorer.service.exception.ResourceNotFoundException;
import com.alejandro.mtoexplorer.service.model.EpOverview;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Paquetes de ejecución, sus estaciones y sus vías.
 */
@ApplicationScoped
public class ExecutionPackageService {

    private final ExecutionPackageRepository executionPackages;
    private final StationRepository stations;
    private final TrackRepository tracks;
    private final ProfileRepository profiles;

    ExecutionPackageService(ExecutionPackageRepository executionPackages, StationRepository stations,
                            TrackRepository tracks, ProfileRepository profiles) {
        this.executionPackages = executionPackages;
        this.stations = stations;
        this.tracks = tracks;
        this.profiles = profiles;
    }

    @WithSession
    public Uni<List<ExecutionPackage>> list() {
        return executionPackages.listAllByCode();
    }

    @WithSession
    public Uni<ExecutionPackage> get(String ep) {
        return existing(ep);
    }

    @WithSession
    public Uni<List<Station>> stations(String ep) {
        return existing(ep).chain(() -> stations.listByEp(ep));
    }

    @WithSession
    public Uni<List<Track>> tracks(String ep) {
        return existing(ep).chain(() -> tracks.listByEp(ep));
    }

    /**
     * Cada paquete con cuántas vías y perfiles tiene. Ejemplo de {@link Multi}: la lista de paquetes
     * se convierte en un flujo, cada elemento lanza sus dos recuentos y el flujo se vuelve a juntar
     * en una lista.
     *
     * <p>{@code transformToUniAndConcatenate} espera a que acabe un paquete antes de empezar el
     * siguiente. {@code transformToUniAndMerge} los lanzaría a la vez, y una sesión de Hibernate
     * Reactive no admite dos consultas a la vez: fallaría.
     *
     * <p>Son 1 + 2 consultas por paquete, y el resultado solo cambia al importar: se cachea. La
     * primera llamada hace las consultas y guarda la lista en Redis; las siguientes la leen de Redis
     * sin abrir sesión. {@code @CacheResult} con un {@code Uni} guarda el valor que emite, no el
     * {@code Uni}, y no guarda los fallos.
     */
    @CacheResult(cacheName = CacheNames.EP_OVERVIEW)
    @WithSession
    public Uni<List<EpOverview>> overview() {
        return executionPackages.listAllByCode()
                .onItem().transformToMulti(eps -> Multi.createFrom().iterable(eps))
                .onItem().transformToUniAndConcatenate(ep -> tracks.countByEp(ep.code)
                        .chain(trackCount -> profiles.countByEp(ep.code)
                                .map(profileCount -> new EpOverview(ep.code, ep.name, ep.enabled,
                                        trackCount, profileCount))))
                .collect().asList();
    }

    /**
     * El resumen de un paquete, sacado de {@link #overview()} y por tanto de la caché. Llamar a
     * {@code overview()} desde aquí pasa por su {@code @CacheResult}: Quarkus intercepta también las
     * llamadas de un bean a sus propios métodos.
     */
    public Uni<EpOverview> overview(String ep) {
        return overview().map(all -> all.stream()
                .filter(o -> o.code().equals(ep))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("el paquete", ep))
        );
    }

    /**
     * El paquete, o ResourceNotFoundException si no existe.
     */
    private Uni<ExecutionPackage> existing(String ep) {
        return executionPackages.findByCode(ep)
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException("el paquete", ep));
    }
}
