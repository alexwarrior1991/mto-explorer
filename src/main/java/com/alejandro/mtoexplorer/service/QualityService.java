package com.alejandro.mtoexplorer.service;

import com.alejandro.mtoexplorer.entity.UnrecognizedValue;
import com.alejandro.mtoexplorer.repository.CantileverRepository;
import com.alejandro.mtoexplorer.repository.ProfileRepository;
import com.alejandro.mtoexplorer.repository.UnrecognizedValueRepository;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import com.alejandro.mtoexplorer.service.cache.CacheNames;
import com.alejandro.mtoexplorer.service.exception.ResourceNotFoundException;
import com.alejandro.mtoexplorer.service.model.EpQuality;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lo que queda por revisar (REVISAR = SI) o por mapear (NO_RECONOCIDO), por paquete. */
@ApplicationScoped
public class QualityService {

    private static final EpCount NONE = new EpCount(null, 0, 0);

    private final ProfileRepository profiles;
    private final CantileverRepository cantilevers;
    private final UnrecognizedValueRepository unrecognized;

    QualityService(ProfileRepository profiles, CantileverRepository cantilevers,
                   UnrecognizedValueRepository unrecognized) {
        this.profiles = profiles;
        this.cantilevers = cantilevers;
        this.unrecognized = unrecognized;
    }

    /**
     * Un resumen por paquete, ordenado por paquete. Tres recuentos (que recorren las tablas de perfiles
     * y ménsulas enteras) seguidos y se cruzan por paquete. Cacheado: sin parámetros, una sola entrada.
     */
    @CacheResult(cacheName = CacheNames.QUALITY)
    @WithSession
    public Uni<List<EpQuality>> perExecutionPackage() {
        return profiles.countPerEp()
                .chain(profileCounts -> cantilevers.countPerEp()
                        .chain(cantileverCounts -> unrecognized.countPerEp()
                                .map(unrecognizedCounts ->
                                        merge(profileCounts, cantileverCounts, unrecognizedCounts))));
    }

    /**
     * La calidad de un paquete. Cacheado por paquete: el parámetro {@code ep} es la clave, así que
     * hay una entrada por cada paquete que se ha pedido ("quality-by-ep" + "EP11A"…). Con varios
     * parámetros, la clave serían todos juntos, o los marcados con {@code @CacheKey}.
     *
     * @throws ResourceNotFoundException (en el {@code Uni}) si el paquete no tiene datos; un fallo
     *                                   no se guarda en la caché
     */
    @CacheResult(cacheName = CacheNames.QUALITY_BY_EP)
    @WithSession
    public Uni<EpQuality> ofExecutionPackage(String ep) {
        return perExecutionPackage()
                .map(all -> all.stream().filter(q -> q.ep().equals(ep)).findFirst().orElse(null))
                .onItem().ifNull().failWith(() -> new ResourceNotFoundException("la calidad del paquete", ep));
    }

    /** La hoja NO_RECONOCIDO, opcionalmente de un maestro (LOV o PROFILE) y de un paquete. */
    @WithSession
    public Uni<List<UnrecognizedValue>> unrecognized(String master, String ep) {
        return unrecognized.search(master == null ? null : master.toUpperCase(), ep);
    }

    private static List<EpQuality> merge(List<EpCount> profileCounts, List<EpCount> cantileverCounts,
                                         List<EpCount> unrecognizedCounts) {
        Map<String, EpCount> byProfile = byEp(profileCounts);
        Map<String, EpCount> byCantilever = byEp(cantileverCounts);
        Map<String, EpCount> byUnrecognized = byEp(unrecognizedCounts);
        TreeSet<String> eps = new TreeSet<>();
        eps.addAll(byProfile.keySet());
        eps.addAll(byCantilever.keySet());
        eps.addAll(byUnrecognized.keySet());
        return eps.stream().map(ep -> {
            EpCount p = byProfile.getOrDefault(ep, NONE);
            EpCount c = byCantilever.getOrDefault(ep, NONE);
            EpCount u = byUnrecognized.getOrDefault(ep, NONE);
            return new EpQuality(ep, p.total(), p.toReview(), c.total(), c.toReview(), u.total());
        }).toList();
    }

    private static Map<String, EpCount> byEp(List<EpCount> counts) {
        return counts.stream().collect(Collectors.toMap(EpCount::ep, Function.identity()));
    }
}