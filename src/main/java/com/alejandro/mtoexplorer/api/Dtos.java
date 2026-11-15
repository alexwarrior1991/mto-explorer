package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.entity.*;
import com.alejandro.mtoexplorer.kp.Kp;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que devuelve la API. Las entidades no salen tal cual: el id interno cambia en cada carga
 * y el PK se quiere también escrito como en los planos.
 */
public final class Dtos {

    private Dtos() {
    }

    public record ExecutionPackageDto(String code, String name, boolean initialPackage, Integer lengthMeters,
                                      LocalDate startDate, LocalDate endDate, String companyId, boolean enabled) {
        static ExecutionPackageDto of(ExecutionPackage e) {
            return new ExecutionPackageDto(e.code, e.name, e.initialPackage, e.lengthMeters,
                    e.startDate, e.endDate, e.companyId, e.enabled);
        }
    }

    public record StationDto(String ep, String name) {
        static StationDto of(Station s) {
            return new StationDto(s.ep, s.name);
        }
    }

    public record TrackDto(String ep, String name, List<String> stations, boolean enabled, String sourceSheet) {
        static TrackDto of(Track t) {
            List<String> stations = t.stations == null ? List.of()
                    : List.of(t.stations.split("\\s*\\|\\s*"));
            return new TrackDto(t.ep, t.name, stations, t.enabled, t.sourceSheet);
        }
    }

    public record ProfileSummary(String ep, String track, int order, String profileId, Double kp, String kpText,
                                 String status, String sectioning, String poleType, String foundation,
                                 Double span, boolean toReview) {
        static ProfileSummary of(Profile p) {
            return new ProfileSummary(p.ep, p.track, p.sortOrder, p.profileId, p.kp, Kp.format(p.kp),
                    p.status, p.sectioning, p.poleType, p.foundation, p.span, p.toReview);
        }
    }

    public record ProfileDetail(String ep, String track, int order, String profileId, Double kp, String kpText,
                                String status, String sectioning, String anchorage, String anchorageFoundation,
                                String foundation, String poleType, String portal, String returnSupport,
                                String sectioningFeeding, String supportType, String assemblyConfiguration,
                                Double span, Double heightCantileverSupport, Double poleGaugeLocation,
                                Double railPoleDistance, boolean enabled, boolean toReview,
                                String sourceSheet, Integer sourceRow, List<CantileverDto> cantilevers) {
        static ProfileDetail of(Profile p, List<Cantilever> cantilevers) {
            return new ProfileDetail(p.ep, p.track, p.sortOrder, p.profileId, p.kp, Kp.format(p.kp),
                    p.status, p.sectioning, p.anchorage, p.anchorageFoundation, p.foundation, p.poleType,
                    p.portal, p.returnSupport, p.sectioningFeeding, p.supportType, p.assemblyConfiguration,
                    p.span, p.heightCantileverSupport, p.poleGaugeLocation, p.railPoleDistance,
                    p.enabled, p.toReview, p.sourceSheet, p.sourceRow,
                    cantilevers.stream().map(CantileverDto::of).toList());
        }
    }

    public record CantileverDto(int slot, String type, Double stagger, Double catenaryHeight, Double cwElevation,
                                Double cwHeight, Double windDeflection, Double armAngle, String steadyArmType,
                                Double steadyArmLength, boolean enabled, boolean toReview) {
        static CantileverDto of(Cantilever c) {
            return new CantileverDto(c.slot, c.type, c.stagger, c.catenaryHeight, c.cwElevation, c.cwHeight,
                    c.windDeflection, c.armAngle, c.steadyArmType, c.steadyArmLength, c.enabled, c.toReview);
        }
    }

    public record LovDto(String entity, String code, String descriptionEn, String descriptionEs, String type,
                         Integer planNumber, boolean enabled, String origin, List<String> eps,
                         Integer usesInTracks, boolean toReview) {
        static LovDto of(Lov l) {
            List<String> eps = l.eps == null ? List.of() : List.of(l.eps.split("\\s*,\\s*"));
            return new LovDto(l.entity, l.code, l.descriptionEn, l.descriptionEs, l.type, l.planNumber,
                    l.enabled, l.origin, eps, l.usesInTracks, l.toReview);
        }
    }

    public record LovEntityCount(String entity, long codes, long toReview) {
    }

    public record UnrecognizedDto(String master, String kind, String value, String ep, String sheet,
                                  Integer firstRow, Integer occurrences) {
        static UnrecognizedDto of(UnrecognizedValue u) {
            return new UnrecognizedDto(u.master, u.kind, u.value, u.ep, u.sheet, u.firstRow, u.occurrences);
        }
    }

    /**
     * Calidad de un paquete: cuánto queda por revisar o por mapear.
     */
    public record EpQuality(String ep, long profiles, long profilesToReview, long cantilevers,
                            long cantileversToReview, long unrecognized) {
    }

    public record DisconnectorDto(String ep, String station, String name, String track, String connectedTrack,
                                  String profileId, Double poleKp, String poleKpText, Double kp, String kpText,
                                  boolean onLoad, Boolean normallyOpen, String driveType, String function,
                                  boolean enabled) {
        static DisconnectorDto of(Disconnector d) {
            return new DisconnectorDto(d.ep, d.station, d.name, d.track, d.connectedTrack, d.profileId,
                    d.poleKp, Kp.format(d.poleKp), d.kp, Kp.format(d.kp), d.onLoad, d.normallyOpen,
                    d.driveType, d.function, d.enabled);
        }
    }

    public record SectionInsulatorDto(String ep, String station, String name, Double kp, String kpText,
                                      String installationType, String track, String connectedTrack,
                                      boolean enabled, List<SwitchDto> switches) {
        static SectionInsulatorDto of(SectionInsulator i, List<SectionInsulatorSwitch> switches) {
            return new SectionInsulatorDto(i.ep, i.station, i.name, i.kp, Kp.format(i.kp), i.installationType,
                    i.track, i.connectedTrack, i.enabled, switches.stream().map(SwitchDto::of).toList());
        }
    }

    /**
     * Una aguja: "W31", con su tangente escrita como en el plano ("1:9").
     */
    public record SwitchDto(String code, Double kp, String kpText, Integer tangent, String tangentText,
                            String track, boolean enabled) {
        static SwitchDto of(SectionInsulatorSwitch w) {
            return new SwitchDto(w.code, w.kp, Kp.format(w.kp), w.tangent,
                    w.tangent == null ? null : "1:" + w.tangent, w.track, w.enabled);
        }
    }

}
