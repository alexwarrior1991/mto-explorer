package com.alejandro.mtoexplorer.service.cache;

import com.alejandro.mtoexplorer.imports.event.MastersImported;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Vacía las cachés cuando cambian los datos de los que salen.
 *
 * <ul>
 *   <li><b>Al importar</b>: escucha el evento {@link MastersImported} y vacía solo las cachés de los
 *       maestros que se han cargado (un maestro UNCHANGED no cambia nada).
 *   <li><b>Al arrancar</b>: Redis no se borra al parar la aplicación, y mientras estaba parada la base
 *       de datos ha podido cambiar (otra versión con otras migraciones, otra carga…). Se vacía todo
 *       antes de importar: lo que haya en Redis de antes no se puede dar por bueno.
 * </ul>
 *
 * <p>Se usa la API programática ({@link CacheManager}) en vez de {@code @CacheInvalidateAll} porque
 * qué cachés se vacían depende del evento. Los métodos esperan ({@code await}) a Redis: los dos se
 * llaman desde hilos que pueden bloquearse (el de arranque y el del importador), nunca desde el event
 * loop.
 */
@ApplicationScoped
public class CacheInvalidator {

    private static final Logger LOG = Logger.getLogger(CacheInvalidator.class);

    private final CacheManager caches;

    CacheInvalidator(CacheManager caches) {
        this.caches = caches;
    }

    /**
     * Antes que la importación del arranque ({@code StartupImport}): un {@code @Priority} más bajo se
     * ejecuta antes. Así, si la importación carga algo, lo que vacía es la caché ya limpia.
     */
    void onStart(@Observes @Priority(1) StartupEvent event){
        invalidate(CacheNames.ALL);
    }

    void onImport(@Observes MastersImported event){
        invalidate(CacheNames.forMasters(event.loadedMasters()));
    }

    /** Vacía esas cachés y espera a que Redis termine. */
    public void invalidate(List<String> names){
        for (String name : names){
            Optional<Cache> cache = caches.getCache(name);
            cache.ifPresent(c -> c.invalidateAll().await().indefinitely());
        }
        if (!names.isEmpty()) {
            LOG.infof("Cachés vaciadas: %s", names);
        }
    }

    /** Vacía todas. */
    public void invalidateAll() {
        invalidate(CacheNames.ALL);
    }
}
