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
import java.util.List;

/** Lee profile-master.xlsx: una hoja por método. */
@ApplicationScoped
public class ProfileMasterReader implements MasterReader<ProfileMaster> {

    public static final String MASTER = "PROFILE";

    @Override
    public ProfileMaster read(MasterWorkbook workbook) {
        return new ProfileMaster(
                executionPackages(workbook),
                stations(workbook),
                tracks(workbook),
                profiles(workbook),
                cantilevers(workbook),
                disconnectors(workbook),
                sectionInsulators(workbook),
                switches(workbook),
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

    private static List<Station> stations(MasterWorkbook workbook) {
        List<Station> stations = new ArrayList<>();
        workbook.forEachRow("STATIONS", r -> {
            Station station = new Station();
            station.ep = r.string("EP");
            station.name = r.string("NOMBRE");
            stations.add(station);
        });
        return stations;
    }

    private static List<Track> tracks(MasterWorkbook workbook) {
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
        return tracks;
    }

    private static List<Profile> profiles(MasterWorkbook workbook) {
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
        return profiles;
    }

    private static List<Cantilever> cantilevers(MasterWorkbook workbook) {
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
        return cantilevers;
    }

    // Las tres hojas del seccionamiento son opcionales: un maestro antiguo no las trae.

    private static List<Disconnector> disconnectors(MasterWorkbook workbook) {
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
        return disconnectors;
    }

    private static List<SectionInsulator> sectionInsulators(MasterWorkbook workbook) {
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
        return insulators;
    }

    private static List<SectionInsulatorSwitch> switches(MasterWorkbook workbook) {
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
        return switches;
    }

    private static int required(Integer value, String column, SheetRow row) {
        if (value == null) {
            throw new MasterFormatException(column, row.rowNumber(), "es obligatoria y está vacía");
        }
        return value;
    }
}
