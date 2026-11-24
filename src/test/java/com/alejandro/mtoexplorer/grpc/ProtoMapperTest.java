package com.alejandro.mtoexplorer.grpc;

import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.entity.Track;
import com.alejandro.mtoexplorer.grpc.v1.ImportStatus;
import com.alejandro.mtoexplorer.grpc.v1.ProfileQuery;
import com.alejandro.mtoexplorer.grpc.v1.ProfileSummary;
import com.alejandro.mtoexplorer.imports.FileImportResult;
import com.alejandro.mtoexplorer.repository.criteria.ProfileSearch;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lo delicado de convertir a protobuf: los null y los optional. JUnit a secas. */
class ProtoMapperTest {

    @Test
    void aNullFieldIsLeftUnset() {
        Profile profile = profile();
        profile.kp = null;
        profile.status = null;

        ProfileSummary summary = ProtoMapper.toSummary(profile);

        assertFalse(summary.hasKp());
        assertFalse(summary.hasKpText());
        assertFalse(summary.hasStatus());
        assertEquals(0.0, summary.getKp(), "sin valor, protobuf devuelve el de por defecto");
    }

    @Test
    void aZeroKpIsSetAndDistinguishableFromMissing() {
        Profile profile = profile();
        profile.kp = 0.0;

        ProfileSummary summary = ProtoMapper.toSummary(profile);

        assertTrue(summary.hasKp());
        assertEquals("0+000", summary.getKpText());
    }

    @Test
    void theQueryOptionalsBecomeNulls() {
        assertEquals(new ProfileSearch("EP1", null, null, null, null),
                ProtoMapper.toSearch(ProfileQuery.newBuilder().setEp("EP1").build()));
        assertEquals(new ProfileSearch("EP1", "TRACK 1", 12345.0, null, true),
                ProtoMapper.toSearch(ProfileQuery.newBuilder().setEp("EP1").setTrack("TRACK 1")
                        .setKpFrom("12+345").setToReview(true).build()));
    }

    @Test
    void timestampsKeepTheNanos() {
        Instant at = Instant.parse("2026-11-19T10:15:30.123456789Z");
        Timestamp timestamp = ProtoMapper.toTimestamp(at);
        assertEquals(at, Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()));
    }

    @Test
    void everyImportStatusHasItsEnumValue() {
        for (FileImportResult.Status status : FileImportResult.Status.values()) {
            assertEquals("IMPORT_STATUS_" + status.name(), ProtoMapper.toProto(status).name());
        }
        assertEquals(ImportStatus.IMPORT_STATUS_LOADED, ProtoMapper.toProto(FileImportResult.Status.LOADED));
    }

    private static Profile profile() {
        ExecutionPackage ep = new ExecutionPackage();
        ep.code = "EP1";
        Track track = new Track();
        track.name = "TRACK 1";
        track.executionPackage = ep;
        Profile profile = new Profile();
        profile.track = track;
        profile.profileId = "1-1.01";
        profile.sortOrder = 1;
        profile.kp = 12000.0;
        profile.status = "A/S";
        return profile;
    }
}
