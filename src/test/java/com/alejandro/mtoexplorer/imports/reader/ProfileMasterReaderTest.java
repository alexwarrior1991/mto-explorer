package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.imports.MasterFixtures;
import com.alejandro.mtoexplorer.imports.excel.MasterWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El lector del maestro de infraestructura, sin Quarkus ni base de datos: un lector es código puro
 * y se prueba con {@code new}.
 */
class ProfileMasterReaderTest {

    @TempDir
    Path dir;

    private final ProfileMasterReader reader = new ProfileMasterReader();

    @Test
    void readsEverySheet() {
        ProfileMaster master = read(true);

        assertEquals(2, master.executionPackages().size());
        assertEquals(3, master.stations().size());
        assertEquals(3, master.tracks().size());
        assertEquals(6, master.profiles().size());
        assertEquals(3, master.cantilevers().size());
        assertEquals(2, master.disconnectors().size());
        assertEquals(2, master.sectionInsulators().size());
        assertEquals(2, master.switches().size());
        assertEquals(2, master.unrecognized().size());
        assertEquals(25, master.rows());
    }

    @Test
    void readsTypesAsTheyAre() {
        ProfileMaster master = read(true);

        ExecutionPackage ep1 = master.executionPackages().getFirst();
        assertEquals("EP1", ep1.code);
        assertTrue(ep1.initialPackage);
        assertEquals(LocalDate.of(2020, 1, 15), ep1.startDate);
        assertNull(master.executionPackages().get(1).startDate);

        Profile second = master.profiles().get(1);
        assertEquals(12054.5, second.kp);
        assertEquals(2, second.sortOrder);
        assertTrue(second.toReview);

        // Los dos perfiles con el mismo PROFILE_ID se leen los dos, con su orden.
        List<Integer> duplicated = master.profiles().stream()
                .filter(p -> p.profileId.equals("1-1.02")).map(p -> p.sortOrder).toList();
        assertEquals(List.of(2, 3), duplicated);

        assertTrue(master.unrecognized().stream().allMatch(u -> u.master.equals(ProfileMasterReader.MASTER)));
    }

    @Test
    void disconnectorWithoutPoleKeepsItsOwnNegativeKpAndUnknownNormallyOpen() {
        Disconnector withoutPole = read(true).disconnectors().get(1);

        assertEquals("NS-01", withoutPole.name);
        assertNull(withoutPole.profileId);
        assertEquals(-250.0, withoutPole.kp);
        assertNull(withoutPole.normallyOpen);
        assertFalse(withoutPole.onLoad);
    }

    @Test
    void anOldMasterWithoutSectioningSheetsStillReads() {
        ProfileMaster master = read(false);

        assertEquals(6, master.profiles().size());
        assertTrue(master.disconnectors().isEmpty());
        assertTrue(master.sectionInsulators().isEmpty());
        assertTrue(master.switches().isEmpty());
    }

    private ProfileMaster read(boolean withSectioning) {
        Path file = dir.resolve("profile-master.xlsx");
        MasterFixtures.writeProfileMaster(file, withSectioning);
        return reader.read(new MasterWorkbook(file));
    }
}
