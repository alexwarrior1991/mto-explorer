package com.alejandro.mtoexplorer.grpc;

import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.grpc.v1.CantileverInfo;
import com.alejandro.mtoexplorer.grpc.v1.ExecutionPackageInfo;
import com.alejandro.mtoexplorer.grpc.v1.ImportNotice;
import com.alejandro.mtoexplorer.grpc.v1.ImportStatus;
import com.alejandro.mtoexplorer.grpc.v1.ProfileInfo;
import com.alejandro.mtoexplorer.grpc.v1.ProfileKey;
import com.alejandro.mtoexplorer.grpc.v1.ProfileLookup;
import com.alejandro.mtoexplorer.grpc.v1.ProfileQuery;
import com.alejandro.mtoexplorer.grpc.v1.ProfileSummary;
import com.alejandro.mtoexplorer.imports.FileImportResult;
import com.alejandro.mtoexplorer.imports.event.ImportEvent;
import com.alejandro.mtoexplorer.kp.Kp;
import com.alejandro.mtoexplorer.repository.criteria.ProfileSearch;
import com.alejandro.mtoexplorer.service.model.EpOverview;
import com.alejandro.mtoexplorer.service.model.ProfileDetail;
import com.google.protobuf.Timestamp;
import opennlp.tools.parser.Cons;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * Entidad / modelo ↔ mensaje de protobuf. Es el {@code DtoMapper} de gRPC.
 *
 * <p>Los mensajes generados son inmutables y se construyen con {@code newBuilder()...build()}.
 * Ojo: <b>un setter de protobuf no admite null</b> ({@code setKp(null)} lanza
 * NullPointerException). Un campo {@code optional} que no tiene valor no se pone, y quien lo recibe
 * lo sabe con {@code hasKp()}. Para eso está {@link #ifPresent}.
 *
 * <p>Como {@code DtoMapper}, lee relaciones LAZY ({@code profile.track.executionPackage.code}): solo
 * funciona porque las consultas del repositorio las traen con {@code join fetch}.
 */
final class ProtoMapper {

    private ProtoMapper() {
    }

    static ExecutionPackageInfo toProto(EpOverview ep) {
        return ExecutionPackageInfo.newBuilder()
                .setCode(ep.code())
                .setName(ep.name())
                .setEnabled(ep.enabled())
                .setTracks(ep.tracks())
                .setProfiles(ep.profiles())
                .build();
    }

    static ProfileSummary toSummary(Profile p) {
        ProfileSummary.Builder b = ProfileSummary.newBuilder()
                .setEp(p.track.executionPackage.code)
                .setTrack(p.track.name)
                .setOrder(p.sortOrder)
                .setProfileId(p.profileId)
                .setToReview(p.toReview);
        ifPresent(p.kp, b::setKp);
        ifPresent(Kp.format(p.kp), b::setKpText);
        ifPresent(p.status, b::setStatus);
        ifPresent(p.sectioning, b::setSectioning);
        ifPresent(p.poleType, b::setPoleType);
        ifPresent(p.foundation, b::setFoundation);
        ifPresent(p.span, b::setSpan);
        return b.build();
    }

    static ProfileInfo toProto(ProfileDetail detail) {
        return ProfileInfo.newBuilder()
                .setSummary(toSummary(detail.profile()))
                .addAllCantilevers(detail.cantilevers().stream().map(ProtoMapper::toProto).toList())
                .addAllDisconnectors(detail.disconnectors().stream().map(d -> d.name).toList())
                .build();
    }

    static CantileverInfo toProto(Cantilever c) {
        CantileverInfo.Builder b = CantileverInfo.newBuilder()
                .setSlot(c.slot)
                .setToReview(c.toReview);
        ifPresent(c.type, b::setType);
        ifPresent(c.stagger, b::setStagger);
        ifPresent(c.catenaryHeight, b::setCatenaryHeight);
        ifPresent(c.cwHeight, b::setCwHeight);
        ifPresent(c.armAngle, b::setArmAngle);
        ifPresent(c.steadyArmType, b::setSteadyArmType);
        return b.build();
    }

    static ProfileLookup found(ProfileKey key, ProfileDetail detail) {
        return ProfileLookup.newBuilder().setKey(key).setProfile(toProto(detail)).build();
    }

    static ProfileLookup notFound(ProfileKey key, String reason) {
        return ProfileLookup.newBuilder().setKey(key).setError(reason).build();
    }

    static ImportNotice toProto(ImportEvent event) {
        FileImportResult r = event.result();
        ImportNotice.Builder b = ImportNotice.newBuilder()
                .setAt(toTimestamp(event.at()))
                .setMaster(r.master())
                .setFile(r.file())
                .setStatus(toProto(r.status()))
                .setRows(r.rows())
                .setDurationMs(r.durationMs());
        ifPresent(r.detail(), b::setDetail);
        return b.build();
    }

    static ImportStatus toProto(FileImportResult.Status status) {
        return switch (status) {
            case LOADED -> ImportStatus.IMPORT_STATUS_LOADED;
            case UNCHANGED -> ImportStatus.IMPORT_STATUS_UNCHANGED;
            case MISSING -> ImportStatus.IMPORT_STATUS_MISSING;
        };
    }

    /**
     * La búsqueda de la petición. Un {@code optional} que no viene es null; el PK, de "12+345" a
     * metros (lanza {@code InvalidKpException}, que {@link GrpcExceptionMapper} convierte en
     * INVALID_ARGUMENT).
     */
    static ProfileSearch toSearch(ProfileQuery q) {
        return new ProfileSearch(q.getEp(),
                q.hasTrack() && !q.getTrack().isBlank() ? q.getTrack() : null,
                q.hasKpFrom() ? Kp.parse(q.getKpFrom()) : null,
                q.hasKpTo() ? Kp.parse(q.getKpTo()) : null,
                q.hasToReview() ? q.getToReview() : null);
    }


    static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    /**
     * Pone el campo solo si hay valor: protobuf no admite null.
     */
    private static <T> void ifPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
