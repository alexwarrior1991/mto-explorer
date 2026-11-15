package com.alejandro.mtoexplorer.load;

import com.alejandro.mtoexplorer.config.ExplorerConfig;
import com.alejandro.mtoexplorer.entity.*;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Carga los dos maestros en la base de datos. Cada fichero va en su propia transacción:
 * borra lo que había de ese maestro y escribe lo que trae el Excel.
 *
 * <p>Es BLOQUEANTE a propósito: leer un Excel es E/S bloqueante. Se llama desde hilos que
 * pueden bloquearse (el de arranque, un endpoint @Blocking, el Scheduler) y entra en
 * Hibernate Reactive con VertxContextSupport.subscribeAndAwait. Nunca desde el event loop.
 */
@ApplicationScoped
public class MasterLoader {

    private static final Logger LOG = Logger.getLogger(MasterLoader.class);

    private final ExplorerConfig config;

    MasterLoader(ExplorerConfig config) {
        this.config = config;
    }

    /**
     * Carga los ficheros que han cambiado desde su última carga (o que no se han cargado nunca).
     */
    public List<FileLoadResult> reloadIfChanged() {
        return reload(false);
    }

    /**
     * Carga los dos ficheros aunque no hayan cambiado.
     */
    public List<FileLoadResult> reloadAll() {
        return reload(true);
    }

    private synchronized List<FileLoadResult> reload(boolean force) {
        Optional<Path> dataDir = config.dataDir();
        if (dataDir.isEmpty()) {
            LOG.warn("mto.data-dir (MTO_DATA_DIR) no está definido: no hay maestros que cargar");
            return List.of(
                    FileLoadResult.missing(config.lovFile(), "mto.data-dir no definido"),
                    FileLoadResult.missing(config.profileFile(), "mto.data-dir no definido")
            );
        }

        // El de LOVs primero, como en mto-configuration: los perfiles usan sus códigos.
        return List.of(
                load(dataDir.get().resolve(config.lovFile()), force, this::readLovMaster),
                load(dataDir.get().resolve(config.profileFile()), force, this::readProfileMaster));
    }

    /**
     * Lo que sale de leer un maestro: cuántas filas y la transacción que lo guarda.
     */
    private record ReadMaster(int rows, Supplier<Uni<Void>> replace) {
    }

    private interface MasterReader {
        ReadMaster read(MasterWorkbook workbook);
    }

    private FileLoadResult load(Path file, boolean force, MasterReader reader) {
        String name = file.getFileName().toString();
        if (!Files.isRegularFile(file)) {
            LOG.warnf("No existe %s", file);
            return FileLoadResult.missing(name, "no existe " + file);
        }
        Instant lastModified = lastModified(file);
        long size = size(file);

        if (!force && unchangedSinceLastLoad(name, lastModified, size)) {
            LOG.infof("%s no ha cambiado desde la última carga", name);
            return FileLoadResult.unchanged(name);
        }

        long start = System.nanoTime();
        // 1. Leer el Excel (bloqueante, sin tocar la base de datos).
        ReadMaster read = reader.read(new MasterWorkbook(file));
        // 2. Guardarlo todo en una transacción reactiva, y esperar a que acabe.
        MasterLoad load = new MasterLoad();
        load.fileName = name;
        load.lastModified = lastModified;
        load.sizeBytes = size;
        load.rowCount = read.rows();
        await(() -> Panache.withTransaction(() -> read.replace().get().chain(() -> {
                            load.loadedAt = Instant.now();
                            load.durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
                            return load.persist();
                        }
                ))
        );
        LOG.infof("Cargado %s: %d filas en %d ms", name, read.rows(), load.durationMs);
        return FileLoadResult.loaded(name, read.rows(), load.durationMs);
    }

    private static boolean unchangedSinceLastLoad(String name, Instant lastModified, long size) {
        MasterLoad last = await(() -> Panache.withSession(() -> MasterLoad.latest(name)));
        return last != null && last.sizeBytes == size && last.lastModified.equals(lastModified);
    }

    // --- lov-master.xlsx -----------------------------------------------------------------

    private ReadMaster readLovMaster(MasterWorkbook workbook) {
        List<Lov> lovs = new ArrayList<>();
        workbook.forEachRow("LOVS", r -> {
            Lov lov = new Lov();
            lov.entity = r.string("ENTIDAD");
            lov.code = r.string("CODIGO");
            lov.descriptionEn = r.string("DESCRIPCION_EN");
            lov.descriptionEs = r.string("DESCRIPCION_ES");
            lov.type = r.string("TIPO");
            lov.planNumber = r.integer("N_PLANO");
            lov.enabled = r.yesNo("ENABLED");
            lov.origin = r.string("ORIGEN");
            lov.boqCategories = r.string("CATEGORIAS_BOQ");
            lov.eps = r.string("EPS");
            lov.usesInTracks = r.integer("USOS_EN_TRACKS");
            lov.toReview = r.yesNo("REVISAR");
            lovs.add(lov);
        });
        List<UnrecognizedValue> unrecognized = readUnrecognized(workbook, "LOV");

        return new ReadMaster(lovs.size() + unrecognized.size(), () -> Lov.deleteAll()
                .chain(() -> UnrecognizedValue.delete("master", "LOV"))
                .chain(() -> persistAll(lovs))
                .chain(() -> persistAll(unrecognized)));
    }

    // --- profile-master.xlsx -------------------------------------------------------------

    private ReadMaster readProfileMaster(MasterWorkbook workbook) {
        List<ExecutionPackage> eps = new ArrayList<>();
        workbook.forEachRow("EPS", r -> {
            ExecutionPackage ep = new ExecutionPackage();
            ep.code = r.string("EP");
            ep.name = r.string("NOMBRE");
            ep.initialPackage = r.yesNo("INITIAL_PACKAGE");
            ep.lengthMeters = r.integer("LENGTH");
            ep.startDate = r.date("START_DATE");
            ep.endDate = r.date("END_DATE");
            ep.companyId = r.string("COMPANY_ID_NUMBER");
            ep.enabled = r.yesNo("ENABLED");
            eps.add(ep);
        });

        List<Station> stations = new ArrayList<>();
        workbook.forEachRow("STATIONS", r -> {
            Station station = new Station();
            station.ep = r.string("EP");
            station.name = r.string("NOMBRE");
            stations.add(station);
        });

        List<Track> tracks = new ArrayList<>();
        workbook.forEachRow("TRACKS", r -> {
            Track track = new Track();
            track.ep = r.string("EP");
            track.name = r.string("NOMBRE");
            track.stations = r.string("ESTACIONES");
            track.enabled = r.yesNo("ENABLED");
            track.sourceSheet = r.string("HOJA_ORIGEN");
            tracks.add(track);
        });

        List<Profile> profiles = new ArrayList<>();
        workbook.forEachRow("PROFILES", r -> {
            Profile p = new Profile();
            p.ep = r.string("EP");
            p.track = r.string("VIA");
            p.profileId = r.string("PROFILE_ID");
            p.kp = r.decimal("KP");
            p.sortOrder = required(r.integer("ORDEN"), "ORDEN", r);
            p.status = r.string("PROFILE_STATUS");
            p.sectioning = r.string("SECTIONING");
            p.anchorage = r.string("ANCHORAGE");
            p.anchorageFoundation = r.string("ANCHORAGE_FOUNDATION");
            p.foundation = r.string("FOUNDATION");
            p.poleType = r.string("POLE_TYPE");
            p.portal = r.string("PORTAL");
            p.returnSupport = r.string("RETURN_SUPPORT");
            p.sectioningFeeding = r.string("SECTIONING_FEEDING");
            p.supportType = r.string("SUPPORT_TYPE");
            p.assemblyConfiguration = r.string("ASSEMBLY_CONFIGURATION");
            p.span = r.decimal("SPAN");
            p.heightCantileverSupport = r.decimal("HEIGHT_CANTILEVER_SUPPORT");
            p.poleGaugeLocation = r.decimal("POLE_GAUGE_LOCATION");
            p.railPoleDistance = r.decimal("RAIL_POLE_DISTANCE");
            p.enabled = r.yesNo("ENABLED");
            p.toReview = r.yesNo("REVISAR");
            p.sourceSheet = r.string("HOJA_ORIGEN");
            p.sourceRow = r.integer("FILA_ORIGEN");
            profiles.add(p);
        });

        List<Cantilever> cantilevers = new ArrayList<>();
        workbook.forEachRow("CANTILEVERS", r -> {
            Cantilever c = new Cantilever();
            c.ep = r.string("EP");
            c.track = r.string("VIA");
            c.profileId = r.string("PROFILE_ID");
            c.profileOrder = required(r.integer("ORDEN"), "ORDEN", r);
            c.slot = required(r.integer("SLOT"), "SLOT", r);
            c.type = r.string("CANTILEVER_TYPE");
            c.stagger = r.decimal("STAGGER");
            c.catenaryHeight = r.decimal("CATENARY_HEIGHT");
            c.cwElevation = r.decimal("CW_ELEVATION");
            c.cwHeight = r.decimal("CW_HEIGHT");
            c.windDeflection = r.decimal("WIND_DEFLECTION");
            c.armAngle = r.decimal("ARM_ANGLE");
            c.steadyArmType = r.string("STEADY_ARM_TYPE");
            c.steadyArmLength = r.decimal("STEADY_ARM_LENGTH");
            c.enabled = r.yesNo("ENABLED");
            c.toReview = r.yesNo("REVISAR");
            c.sourceRow = r.integer("FILA_ORIGEN");
            cantilevers.add(c);
        });

        // Las tres hojas del seccionamiento son opcionales: un maestro antiguo no las trae.
        List<Disconnector> disconnectors = new ArrayList<>();
        workbook.forEachRowIfPresent("DISCONNECTORS", r -> {
            Disconnector d = new Disconnector();
            d.ep = r.string("EP");
            d.station = r.string("ESTACION");
            d.track = r.string("VIA");
            d.connectedTrack = r.string("VIA_CONECTADA");
            d.profileId = r.string("PROFILE_ID");
            d.poleKp = r.decimal("KP_POSTE");
            d.name = r.string("NOMBRE");
            d.kp = r.decimal("KP");
            d.onLoad = r.yesNo("ON_LOAD");
            d.normallyOpen = r.optionalYesNo("NORMALLY_OPEN");
            d.driveType = r.string("DRIVE_TYPE");
            d.function = r.string("DISCONNECTOR_FUNCTION");
            d.enabled = r.yesNo("ENABLED");
            disconnectors.add(d);
        });

        List<SectionInsulator> insulators = new ArrayList<>();
        workbook.forEachRowIfPresent("SECTION_INSULATORS", r -> {
            SectionInsulator i = new SectionInsulator();
            i.ep = r.string("EP");
            i.station = r.string("ESTACION");
            i.name = r.string("NOMBRE");
            i.kp = r.decimal("KP");
            i.installationType = r.string("TIPO_INSTALACION");
            i.track = r.string("VIA");
            i.connectedTrack = r.string("VIA_CONECTADA");
            i.enabled = r.yesNo("ENABLED");
            insulators.add(i);
        });

        List<SectionInsulatorSwitch> switches = new ArrayList<>();
        workbook.forEachRowIfPresent("SECTION_INSULATOR_SWITCHES", r -> {
            SectionInsulatorSwitch w = new SectionInsulatorSwitch();
            w.ep = r.string("EP");
            w.station = r.string("ESTACION");
            w.insulator = r.string("AISLADOR");
            w.code = r.string("CODIGO");
            w.kp = r.decimal("KP");
            w.tangent = r.integer("TANGENTE");
            w.track = r.string("VIA");
            w.enabled = r.yesNo("ENABLED");
            switches.add(w);
        });

        List<UnrecognizedValue> unrecognized = readUnrecognized(workbook, "PROFILE");

        int rows = eps.size() + stations.size() + tracks.size() + profiles.size() + cantilevers.size()
                + disconnectors.size() + insulators.size() + switches.size() + unrecognized.size();
        return new ReadMaster(rows, () -> SectionInsulatorSwitch.deleteAll()
                .chain(() -> SectionInsulator.deleteAll())
                .chain(() -> Disconnector.deleteAll())
                .chain(() -> Cantilever.deleteAll())
                .chain(() -> Profile.deleteAll())
                .chain(() -> Track.deleteAll())
                .chain(() -> Station.deleteAll())
                .chain(() -> ExecutionPackage.deleteAll())
                .chain(() -> UnrecognizedValue.delete("master", "PROFILE"))
                .chain(() -> persistAll(eps))
                .chain(() -> persistAll(stations))
                .chain(() -> persistAll(tracks))
                .chain(() -> persistAll(profiles))
                .chain(() -> persistAll(cantilevers))
                .chain(() -> persistAll(disconnectors))
                .chain(() -> persistAll(insulators))
                .chain(() -> persistAll(switches))
                .chain(() -> persistAll(unrecognized)));
    }

    private static List<UnrecognizedValue> readUnrecognized(MasterWorkbook workbook, String master) {
        List<UnrecognizedValue> values = new ArrayList<>();
        workbook.forEachRow("NO_RECONOCIDO", r -> {
            UnrecognizedValue u = new UnrecognizedValue();
            u.master = master;
            u.kind = r.string("TIPO");
            u.value = r.string("VALOR");
            u.ep = r.string("EP");
            u.sheet = r.string("HOJA");
            u.firstRow = r.integer("PRIMERA_FILA");
            u.occurrences = r.integer("APARICIONES");
            values.add(u);
        });
        return values;
    }


    /**
     * Persiste una lista entera. Panache falla con una lista vacía ("The Uni set is empty"), y en el
     * maestro real la hoja NO_RECONOCIDO está vacía.
     */
    private static Uni<Void> persistAll(List<?> entities) {
        if (entities.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        return Panache.getSession().chain(session ->
                session.persistAll(entities.toArray()));
    }

    private static int required(Integer value, String column, SheetRow row) {
        if (value == null) {
            throw new MasterFormatException(column, row.rowNumber(), "es obligatoria y está vacía");
        }
        return value;
    }

    /**
     * Ejecuta código reactivo desde un hilo bloqueante y espera su resultado.
     */
    private static <T> T await(Supplier<Uni<T>> work) {
        try {
            return VertxContextSupport.subscribeAndAwait(work);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Instant lastModified(Path file) {
        try {
            // PostgreSQL guarda microsegundos: truncar para que la comparación con la última carga sea estable.
            return Files.getLastModifiedTime(file).toInstant().truncatedTo(ChronoUnit.MILLIS);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
