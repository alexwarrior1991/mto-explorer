package com.alejandro.mtoexplorer.imports;

import com.alejandro.mtoexplorer.config.ExplorerConfig;
import com.alejandro.mtoexplorer.entity.MasterLoad;
import com.alejandro.mtoexplorer.imports.event.MastersImported;
import com.alejandro.mtoexplorer.imports.excel.MasterWorkbook;
import com.alejandro.mtoexplorer.imports.reader.LovMaster;
import com.alejandro.mtoexplorer.imports.reader.LovMasterReader;
import com.alejandro.mtoexplorer.imports.reader.MasterReader;
import com.alejandro.mtoexplorer.imports.reader.ProfileMaster;
import com.alejandro.mtoexplorer.imports.reader.ProfileMasterReader;
import com.alejandro.mtoexplorer.imports.reader.ReadMaster;
import com.alejandro.mtoexplorer.imports.store.MasterStore;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Orquesta la importación de los dos maestros: decide cuáles hay que cargar, los lee y los guarda.
 * No sabe leer Excel (eso es {@code reader}) ni escribir en la base de datos (eso es {@code store}).
 *
 * <p>Al terminar publica un evento CDI {@link MastersImported}: la caché se vacía y el canal SSE avisa
 * sin que este servicio sepa nada de ellos.
 *
 * <p>Es BLOQUEANTE a propósito: leer un Excel es E/S bloqueante. Se llama desde hilos que pueden
 * bloquearse (el de arranque, un endpoint {@code @Blocking}, el Scheduler de la fase 4), y entra en
 * Hibernate Reactive con {@link VertxContextSupport#subscribeAndAwait}. Nunca desde el event loop.
 */
@ApplicationScoped
public class MasterImportService {

    private static final Logger LOG = Logger.getLogger(MasterImportService.class);

    private final ExplorerConfig config;
    private final LovMasterReader lovReader;
    private final ProfileMasterReader profileReader;
    private final MasterStore store;

    private final Event<MastersImported> imported;

    MasterImportService(ExplorerConfig config, LovMasterReader lovReader, ProfileMasterReader profileReader,
                        MasterStore store, Event<MastersImported> imported) {
        this.config = config;
        this.lovReader = lovReader;
        this.profileReader = profileReader;
        this.store = store;
        this.imported = imported;
    }

    /** Importa los ficheros que han cambiado desde su última carga (o que no se han cargado nunca). */
    public List<FileImportResult> importIfChanged() {
        return importMasters(false);
    }

    /** Importa los dos ficheros aunque no hayan cambiado. */
    public List<FileImportResult> importAll() {
        return importMasters(true);
    }

    /** synchronized: dos importaciones a la vez (el arranque y una petición) se pisarían. */
    /**
     * synchronized: dos importaciones a la vez (el arranque y una petición) se pisarían.
     * El evento se publica dentro, para que los avisos salgan en el mismo orden que las importaciones.
     */
    private synchronized List<FileImportResult> importMasters(boolean force) {
        List<FileImportResult> results = importBoth(force);
        imported.fire(new MastersImported(Instant.now(), results));
        return results;
    }

    private List<FileImportResult> importBoth(boolean force) {
        Optional<Path> dataDir = config.dataDir();
        if (dataDir.isEmpty()) {
            LOG.warn("mto.data-dir (MTO_DATA_DIR) no está definido: no hay maestros que importar");
            return List.of(
                    FileImportResult.missing(LovMasterReader.MASTER, config.lovFile(), "mto.data-dir no definido"),
                    FileImportResult.missing(ProfileMasterReader.MASTER, config.profileFile(),
                            "mto.data-dir no definido"));
        }
        // El de LOVs primero, como en mto-configuration: los perfiles usan sus códigos.
        return List.of(
                importMaster(LovMasterReader.MASTER, dataDir.get().resolve(config.lovFile()), force, lovReader,
                        (LovMaster master, Supplier<MasterLoad> load) -> store.replace(master, load)),
                importMaster(ProfileMasterReader.MASTER, dataDir.get().resolve(config.profileFile()), force,
                        profileReader,
                        (ProfileMaster master, Supplier<MasterLoad> load) -> store.replace(master, load)));
    }

    private <M extends ReadMaster> FileImportResult importMaster(
            String masterName, Path path, boolean force, MasterReader<M> reader,
            BiFunction<M, Supplier<MasterLoad>, Uni<MasterLoad>> save) {

        String name = path.getFileName().toString();
        if (!MasterFile.exists(path)) {
            LOG.warnf("No existe %s", path);
            return FileImportResult.missing(masterName, name, "no existe " + path);
        }
        MasterFile file = MasterFile.of(path);
        if (!force && file.sameAs(await(() -> store.latestLoad(name)))) {
            LOG.infof("%s no ha cambiado desde la última carga", name);
            return FileImportResult.unchanged(masterName, name);
        }

        long start = System.nanoTime();
        M master = reader.read(new MasterWorkbook(path));
        MasterLoad saved = await(() -> save.apply(master, () -> loadRecord(file, master.rows(), start)));
        LOG.infof("Importado %s: %d filas en %d ms", name, saved.rowCount, saved.durationMs);
        return FileImportResult.loaded(masterName, name, saved.rowCount, saved.durationMs);
    }

    private static MasterLoad loadRecord(MasterFile file, int rows, long start) {
        MasterLoad load = new MasterLoad();
        load.fileName = file.name();
        load.lastModified = file.lastModified();
        load.sizeBytes = file.size();
        load.rowCount = rows;
        load.loadedAt = Instant.now();
        load.durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        return load;
    }

    /** Ejecuta código reactivo desde un hilo bloqueante y espera su resultado. */
    private static <T> T await(Supplier<Uni<T>> work) {
        try {
            return VertxContextSupport.subscribeAndAwait(work);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
