package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.entity.SectionInsulator;
import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;
import com.alejandro.mtoexplorer.entity.Station;
import com.alejandro.mtoexplorer.entity.Track;
import com.alejandro.mtoexplorer.imports.excel.MasterFormatException;
import com.alejandro.mtoexplorer.imports.excel.MasterWorkbook;
import com.alejandro.mtoexplorer.imports.excel.SheetRow;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee profile-master.xlsx y enlaza las entidades entre sí: cada hijo apunta al objeto de su padre
 * (una vía a su paquete, un perfil a su vía…), y Hibernate escribe las claves foráneas al guardar.
 * Por eso las hojas se leen de padres a hijos, y cada una busca a sus padres en un {@link Lookup}
 * de lo ya leído.
 */
@ApplicationScoped
public class ProfileMasterReader implements MasterReader<ProfileMaster> {

    public static final String MASTER = "PROFILE";

    /** Cómo escribe el maestro un seccionador que no es de ninguna estación (con tilde o sin ella). */
    private static final List<String> NO_STATION = List.of("SIN ESTACION", "SIN ESTACIÓN");

    /** Claves naturales del maestro. El toString es lo que sale en los mensajes de error. */
    private record ByName(String ep, String name) {
        @Override
        public String toString() {
            return ep + " / " + name;
        }
    }

    private record ProfileKey(String ep, String track, int order) {
        @Override
        public String toString() {
            return ep + " / " + track + " / orden " + order;
        }
    }

    private record InsulatorKey(String ep, String station, String name) {
        @Override
        public String toString() {
            return ep + " / " + station + " / " + name;
        }
    }

    @Override
    public ProfileMaster read(MasterWorkbook workbook) {
        List<ExecutionPackage> eps = executionPackages(workbook);
        Lookup<String, ExecutionPackage> epLookup = Lookup.of("el paquete", eps, ep -> ep.code);

        List<Station> stations = stations(workbook, epLookup);
        Lookup<ByName, Station> stationLookup =
                Lookup.of("la estación", stations, s -> new ByName(s.executionPackage.code, s.name));

        List<Track> tracks = tracks(workbook, epLookup, stationLookup);
        Lookup<ByName, Track> trackLookup =
                Lookup.of("la vía", tracks, t -> new ByName(t.executionPackage.code, t.name));

        List<Profile> profiles = profiles(workbook, trackLookup);
        List<Cantilever> cantilevers = cantilevers(workbook, Lookup.of("el perfil", profiles,
                p -> new ProfileKey(p.track.executionPackage.code, p.track.name, p.sortOrder)));

        List<SectionInsulator> insulators = sectionInsulators(workbook, stationLookup, trackLookup);
        List<SectionInsulatorSwitch> switches = switches(workbook, trackLookup, Lookup.of("el aislador", insulators,
                i -> new InsulatorKey(i.station.executionPackage.code, i.station.name, i.name)));

        List<Disconnector> disconnectors =
                disconnectors(workbook, epLookup, stationLookup, trackLookup, polesByProfileId(profiles));

        return new ProfileMaster(eps, stations, tracks, profiles, cantilevers, disconnectors, insulators, switches,
                UnrecognizedSheet.read(workbook, MASTER));
    }

    private static List<ExecutionPackage> executionPackages(MasterWorkbook workbook) {
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
        return eps;
    }

    private static List<Station> stations(MasterWorkbook workbook, Lookup<String, ExecutionPackage> eps) {
        List<Station> stations = new ArrayList<>();
        workbook.forEachRow("STATIONS", r -> {
            Station station = new Station();
            station.executionPackage = eps.require(r.string("EP"), r, "EP");
            station.name = r.string("NOMBRE");
            stations.add(station);
        });
        return stations;
    }

    /** Las estaciones de una vía vienen en una celda, "MOM | PMO": se separan y se busca cada una. */
    private static List<Track> tracks(MasterWorkbook workbook, Lookup<String, ExecutionPackage> eps,
                                      Lookup<ByName, Station> stations) {
        List<Track> tracks = new ArrayList<>();
        workbook.forEachRow("TRACKS", r -> {
            Track track = new Track();
            track.executionPackage = eps.require(r.string("EP"), r, "EP");
            track.name = r.string("NOMBRE");
            String names = r.string("ESTACIONES");
            if (names != null) {
                Arrays.stream(names.split("\\|")).map(String::trim).filter(n -> !n.isEmpty())
                        .forEach(n -> track.stations.add(
                                stations.require(new ByName(track.executionPackage.code, n), r, "ESTACIONES")));
            }
            track.enabled = r.yesNo("ENABLED");
            track.sourceSheet = r.string("HOJA_ORIGEN");
            tracks.add(track);
        });
        return tracks;
    }

    private static List<Profile> profiles(MasterWorkbook workbook, Lookup<ByName, Track> tracks) {
        List<Profile> profiles = new ArrayList<>();
        workbook.forEachRow("PROFILES", r -> {
            Profile p = new Profile();
            p.track = tracks.require(new ByName(r.string("EP"), r.string("VIA")), r, "VIA");
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
        return profiles;
    }

    /**
     * Cada ménsula apunta a su objeto {@link Profile} y se añade a {@code profile.cantilevers}, para
     * que el modelo en memoria sea coherente por los dos lados de la relación.
     */
    private static List<Cantilever> cantilevers(MasterWorkbook workbook, Lookup<ProfileKey, Profile> profiles) {
        List<Cantilever> cantilevers = new ArrayList<>();
        workbook.forEachRow("CANTILEVERS", r -> {
            Cantilever c = new Cantilever();
            c.profile = profiles.require(
                    new ProfileKey(r.string("EP"), r.string("VIA"), required(r.integer("ORDEN"), "ORDEN", r)),
                    r, "ORDEN");
            c.profile.cantilevers.add(c);
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
        return cantilevers;
    }

    // Las tres hojas del seccionamiento son opcionales: un maestro antiguo no las trae.

    private static List<SectionInsulator> sectionInsulators(MasterWorkbook workbook, Lookup<ByName, Station> stations,
                                                            Lookup<ByName, Track> tracks) {
        List<SectionInsulator> insulators = new ArrayList<>();
        workbook.forEachRowIfPresent("SECTION_INSULATORS", r -> {
            String ep = r.string("EP");
            SectionInsulator i = new SectionInsulator();
            i.station = stations.require(new ByName(ep, r.string("ESTACION")), r, "ESTACION");
            i.name = r.string("NOMBRE");
            i.kp = r.decimal("KP");
            i.installationType = r.string("TIPO_INSTALACION");
            i.track = tracks.optional(byName(ep, r.string("VIA")), r, "VIA");
            i.connectedTrack = tracks.optional(byName(ep, r.string("VIA_CONECTADA")), r, "VIA_CONECTADA");
            i.enabled = r.yesNo("ENABLED");
            insulators.add(i);
        });
        return insulators;
    }

    private static List<SectionInsulatorSwitch> switches(MasterWorkbook workbook, Lookup<ByName, Track> tracks,
                                                         Lookup<InsulatorKey, SectionInsulator> insulators) {
        List<SectionInsulatorSwitch> switches = new ArrayList<>();
        workbook.forEachRowIfPresent("SECTION_INSULATOR_SWITCHES", r -> {
            String ep = r.string("EP");
            SectionInsulatorSwitch w = new SectionInsulatorSwitch();
            w.insulator = insulators.require(new InsulatorKey(ep, r.string("ESTACION"), r.string("AISLADOR")),
                    r, "AISLADOR");
            w.insulator.switches.add(w);
            w.code = r.string("CODIGO");
            w.kp = r.decimal("KP");
            w.tangent = r.integer("TANGENTE");
            w.track = tracks.optional(byName(ep, r.string("VIA")), r, "VIA");
            w.enabled = r.yesNo("ENABLED");
            switches.add(w);
        });
        return switches;
    }

    /** Los perfiles de cada paquete por su PROFILE_ID, que se repite: varios candidatos a poste. */
    private static Map<ByName, List<Profile>> polesByProfileId(List<Profile> profiles) {
        Map<ByName, List<Profile>> poles = new HashMap<>();
        for (Profile p : profiles) {
            poles.computeIfAbsent(new ByName(p.track.executionPackage.code, p.profileId), k -> new ArrayList<>())
                    .add(p);
        }
        return poles;
    }

    private static List<Disconnector> disconnectors(MasterWorkbook workbook, Lookup<String, ExecutionPackage> eps,
                                                    Lookup<ByName, Station> stations, Lookup<ByName, Track> tracks,
                                                    Map<ByName, List<Profile>> poles) {
        List<Disconnector> disconnectors = new ArrayList<>();
        workbook.forEachRowIfPresent("DISCONNECTORS", r -> {
            String ep = r.string("EP");
            Disconnector d = new Disconnector();
            d.executionPackage = eps.require(ep, r, "EP");
            String station = r.string("ESTACION");
            d.station = station == null || NO_STATION.contains(station.toUpperCase())
                    ? null : stations.require(new ByName(ep, station), r, "ESTACION");
            d.pole = pole(r, ep, poles);
            if (d.pole != null) {
                d.pole.disconnectors.add(d);
            }
            // Sin VIA, la vía del seccionador es la de su poste.
            Track track = tracks.optional(byName(ep, r.string("VIA")), r, "VIA");
            d.track = track != null || d.pole == null ? track : d.pole.track;
            d.connectedTrack = tracks.optional(byName(ep, r.string("VIA_CONECTADA")), r, "VIA_CONECTADA");
            d.name = r.string("NOMBRE");
            d.kp = r.decimal("KP");
            d.onLoad = r.yesNo("ON_LOAD");
            d.normallyOpen = r.optionalYesNo("NORMALLY_OPEN");
            d.driveType = r.string("DRIVE_TYPE");
            d.function = r.string("DISCONNECTOR_FUNCTION");
            d.enabled = r.yesNo("ENABLED");
            disconnectors.add(d);
        });
        return disconnectors;
    }

    /** El poste del seccionador de esta fila: null sin PROFILE_ID; error si no existe o no se puede desempatar. */
    private static Profile pole(SheetRow r, String ep, Map<ByName, List<Profile>> poles) {
        String profileId = r.string("PROFILE_ID");
        if (profileId == null) {
            return null;
        }
        List<Profile> candidates = poles.getOrDefault(new ByName(ep, profileId), List.of());
        return PoleResolver.resolve(candidates, r.decimal("KP_POSTE"), r.string("VIA"))
                .orElseThrow(() -> new MasterFormatException("PROFILE_ID", r.rowNumber(), candidates.isEmpty()
                        ? "no existe el poste " + profileId + " en " + ep
                        : "hay " + candidates.size() + " postes " + profileId + " en " + ep
                        + " y ni KP_POSTE ni VIA dicen cuál es"));
    }

    /** La clave de una vía, o null si la celda está vacía (para {@link Lookup#optional}). */
    private static ByName byName(String ep, String name) {
        return name == null ? null : new ByName(ep, name);
    }

    private static int required(Integer value, String column, SheetRow row) {
        if (value == null) {
            throw new MasterFormatException(column, row.rowNumber(), "es obligatoria y está vacía");
        }
        return value;
    }
}
