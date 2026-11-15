package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.entity.Track;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las reglas para saber de qué poste cuelga un seccionador cuando el PROFILE_ID se repite. */
class PoleResolverTest {

    private final Track track1 = track("TRACK 1");
    private final Track track2 = track("TRACK 2");

    @Test
    void noCandidatesIsNoPole() {
        assertTrue(PoleResolver.resolve(List.of(), 100.0, "TRACK 1").isEmpty());
    }

    @Test
    void aSingleCandidateIsThePoleWhateverKpAndTrackSay() {
        Profile only = profile(track1, 100.0);
        assertEquals(Optional.of(only), PoleResolver.resolve(List.of(only), 999.0, "TRACK 9"));
    }

    @Test
    void severalCandidatesAreResolvedByKp() {
        Profile first = profile(track1, 100.0);
        Profile second = profile(track1, 2100.0);
        assertEquals(Optional.of(second), PoleResolver.resolve(List.of(first, second), 2100.004, "TRACK 1"));
    }

    @Test
    void aJunctionPoleInTwoTracksAtTheSameKpIsResolvedByTrack() {
        Profile onTrack1 = profile(track1, 500.0);
        Profile onTrack2 = profile(track2, 500.0);
        assertEquals(Optional.of(onTrack2), PoleResolver.resolve(List.of(onTrack1, onTrack2), 500.0, "TRACK 2"));
    }

    @Test
    void aKpThatMatchesNoCandidateFallsBackToTheTrack() {
        // El caso real de EP6 / 92-4.20: KP_POSTE 92718.65 y los dos postes en el 92665.05.
        Profile onTrack4 = profile(track("TRACK 4"), 92665.05);
        Profile onTrack6 = profile(track("TRACK 6"), 92665.05);
        assertEquals(Optional.of(onTrack4),
                PoleResolver.resolve(List.of(onTrack4, onTrack6), 92718.65, "TRACK 4"));
    }

    @Test
    void whatCannotBeTiedIsNoPole() {
        Profile a = profile(track1, 500.0);
        Profile b = profile(track1, 500.0);
        assertTrue(PoleResolver.resolve(List.of(a, b), 500.0, "TRACK 1").isEmpty());
        assertTrue(PoleResolver.resolve(List.of(a, b), null, null).isEmpty());
    }

    private static Track track(String name) {
        Track track = new Track();
        track.name = name;
        return track;
    }

    private static Profile profile(Track track, Double kp) {
        Profile profile = new Profile();
        profile.track = track;
        profile.kp = kp;
        return profile;
    }
}
