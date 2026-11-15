package com.alejandro.mtoexplorer.api.mapper;

import com.alejandro.mtoexplorer.api.dto.CantileverDto;
import com.alejandro.mtoexplorer.api.dto.DisconnectorDto;
import com.alejandro.mtoexplorer.api.dto.EpOverviewDto;
import com.alejandro.mtoexplorer.api.dto.EpQualityDto;
import com.alejandro.mtoexplorer.api.dto.ExecutionPackageDto;
import com.alejandro.mtoexplorer.api.dto.ImportResultDto;
import com.alejandro.mtoexplorer.api.dto.LovDto;
import com.alejandro.mtoexplorer.api.dto.LovFamilyDto;
import com.alejandro.mtoexplorer.api.dto.MasterLoadDto;
import com.alejandro.mtoexplorer.api.dto.PageDto;
import com.alejandro.mtoexplorer.api.dto.ProfileDetailDto;
import com.alejandro.mtoexplorer.api.dto.ProfileDisconnectorDto;
import com.alejandro.mtoexplorer.api.dto.ProfileSummaryDto;
import com.alejandro.mtoexplorer.api.dto.SectionInsulatorDto;
import com.alejandro.mtoexplorer.api.dto.StationDto;
import com.alejandro.mtoexplorer.api.dto.SwitchDto;
import com.alejandro.mtoexplorer.api.dto.TrackDto;
import com.alejandro.mtoexplorer.api.dto.UnrecognizedDto;
import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import com.alejandro.mtoexplorer.entity.Lov;
import com.alejandro.mtoexplorer.entity.MasterLoad;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.entity.SectionInsulator;
import com.alejandro.mtoexplorer.entity.SectionInsulatorSwitch;
import com.alejandro.mtoexplorer.entity.Station;
import com.alejandro.mtoexplorer.entity.Track;
import com.alejandro.mtoexplorer.entity.UnrecognizedValue;
import com.alejandro.mtoexplorer.imports.FileImportResult;
import com.alejandro.mtoexplorer.kp.Kp;
import com.alejandro.mtoexplorer.repository.DisconnectorRepository;
import com.alejandro.mtoexplorer.repository.projection.LovFamilyCount;
import com.alejandro.mtoexplorer.service.model.EpOverview;
import com.alejandro.mtoexplorer.service.model.EpQuality;
import com.alejandro.mtoexplorer.service.model.Page;
import com.alejandro.mtoexplorer.service.model.ProfileDetail;

import java.util.List;
import java.util.function.Function;

/**
 * De entidades y modelos de servicio a DTO. Todo el formato de presentación vive aquí: el PK como
 * "17+858.5", la tangente como "1:9", las relaciones como el nombre del padre.
 *
 * <p>Ojo: lee relaciones LAZY ({@code station.executionPackage.code}, {@code profile.track.name}…).
 * Solo funciona porque las consultas de los repositorios las traen con {@code join fetch}; una
 * relación sin cargar fallaría aquí con LazyInitializationException.
 *
 * <p>En un proyecto con muchas entidades esto lo generaría MapStruct (como en mto-configuration);
 * aquí, a mano, se ve qué hace cada conversión.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static <T, R> PageDto<R> toDto(Page<T> page, Function<T, R> mapper) {
        Page<R> mapped = page.map(mapper);
        return new PageDto<>(mapped.items(), mapped.page(), mapped.size(), mapped.total());
    }

    public static <T, R> List<R> toDtos(List<T> items, Function<T, R> mapper) {
        return items.stream().map(mapper).toList();
    }

    public static ExecutionPackageDto toDto(ExecutionPackage e) {
        return new ExecutionPackageDto(e.code, e.name, e.initialPackage, e.lengthMeters,
                e.startDate, e.endDate, e.companyId, e.enabled);
    }

    public static EpOverviewDto toDto(EpOverview o) {
        ExecutionPackage e = o.executionPackage();
        return new EpOverviewDto(e.code, e.name, e.enabled, o.tracks(), o.profiles());
    }

    public static StationDto toDto(Station s) {
        return new StationDto(s.executionPackage.code, s.name);
    }

    public static TrackDto toDto(Track t) {
        return new TrackDto(t.executionPackage.code, t.name, t.stations.stream().map(s -> s.name).toList(),
                t.enabled, t.sourceSheet);
    }

    public static ProfileSummaryDto toSummary(Profile p) {
        return new ProfileSummaryDto(p.track.executionPackage.code, p.track.name, p.sortOrder, p.profileId, p.kp, Kp.format(p.kp),
                p.status, p.sectioning, p.poleType, p.foundation, p.span, p.toReview);
    }

    public static ProfileDetailDto toDto(ProfileDetail detail) {
        Profile p = detail.profile();
        return new ProfileDetailDto(p.track.executionPackage.code, p.track.name, p.sortOrder, p.profileId, p.kp, Kp.format(p.kp),
                p.status, p.sectioning, p.anchorage, p.anchorageFoundation, p.foundation, p.poleType,
                p.portal, p.returnSupport, p.sectioningFeeding, p.supportType, p.assemblyConfiguration,
                p.span, p.heightCantileverSupport, p.poleGaugeLocation, p.railPoleDistance,
                p.enabled, p.toReview, p.sourceSheet, p.sourceRow,
                toDtos(detail.cantilevers(), DtoMapper::toDto),
                toDtos(detail.disconnectors(), d -> new ProfileDisconnectorDto(d.name, d.function, d.onLoad,
                        d.normallyOpen, d.enabled)));
    }

    public static CantileverDto toDto(Cantilever c) {
        return new CantileverDto(c.slot, c.type, c.stagger, c.catenaryHeight, c.cwElevation, c.cwHeight,
                c.windDeflection, c.armAngle, c.steadyArmType, c.steadyArmLength, c.enabled, c.toReview);
    }

    public static LovDto toDto(Lov l) {
        return new LovDto(l.entity, l.code, l.descriptionEn, l.descriptionEs, l.type, l.planNumber,
                l.enabled, l.origin, split(l.eps, "\\s*,\\s*"), l.usesInTracks, l.toReview);
    }

    public static LovFamilyDto toDto(LovFamilyCount f) {
        return new LovFamilyDto(f.entity(), f.codes(), f.toReview());
    }

    public static UnrecognizedDto toDto(UnrecognizedValue u) {
        return new UnrecognizedDto(u.master, u.kind, u.value, u.ep, u.sheet, u.firstRow, u.occurrences);
    }

    public static EpQualityDto toDto(EpQuality q) {
        return new EpQualityDto(q.ep(), q.profiles(), q.profilesToReview(), q.cantilevers(),
                q.cantileversToReview(), q.unrecognized());
    }

    public static DisconnectorDto toDto(Disconnector d) {
        Profile pole = d.pole;
        Double poleKp = pole == null ? null : pole.kp;
        return new DisconnectorDto(d.executionPackage.code,
                d.station == null ? DisconnectorRepository.NO_STATION : d.station.name,
                d.name, name(d.track), name(d.connectedTrack),
                pole == null ? null : pole.profileId, pole == null ? null : pole.track.name,
                pole == null ? null : pole.sortOrder, poleKp, Kp.format(poleKp),
                d.kp, Kp.format(d.kp), d.onLoad, d.normallyOpen, d.driveType, d.function, d.enabled);
    }

    public static SectionInsulatorDto toDto(SectionInsulator i) {
        return new SectionInsulatorDto(i.station.executionPackage.code, i.station.name, i.name, i.kp,
                Kp.format(i.kp), i.installationType, name(i.track), name(i.connectedTrack), i.enabled,
                toDtos(i.switches, DtoMapper::toDto));
    }

    public static SwitchDto toDto(SectionInsulatorSwitch w) {
        return new SwitchDto(w.code, w.kp, Kp.format(w.kp), w.tangent,
                w.tangent == null ? null : "1:" + w.tangent, name(w.track), w.enabled);
    }

    public static MasterLoadDto toDto(MasterLoad l) {
        return new MasterLoadDto(l.fileName, l.lastModified, l.sizeBytes, l.loadedAt, l.rowCount, l.durationMs);
    }

    public static ImportResultDto toDto(FileImportResult r) {
        return new ImportResultDto(r.file(), r.status().name(), r.rows(), r.durationMs(), r.detail());
    }

    /** El nombre de una vía opcional. */
    private static String name(Track track) {
        return track == null ? null : track.name;
    }

    private static List<String> split(String value, String separator) {
        return value == null ? List.of() : List.of(value.split(separator));
    }
}
