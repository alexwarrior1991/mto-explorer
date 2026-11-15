package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.imports.MasterFixtures;
import com.alejandro.mtoexplorer.imports.excel.MasterFormatException;
import com.alejandro.mtoexplorer.imports.excel.MasterWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void eachCantileverPointsToItsProfileObjectAndTheProfileListsIt() {
        ProfileMaster master = read(true);
        // EP1 / TRACK 1: el perfil de orden 2 tiene dos ménsulas; el de orden 3 comparte PROFILE_ID y no tiene.
        Profile second = master.profiles().get(1);
        Profile third = master.profiles().get(2);

        List<Cantilever> ofSecond = master.cantilevers().stream().filter(c -> c.profile == second).toList();
        assertEquals(2, ofSecond.size());
        assertEquals(ofSecond, second.cantilevers);
        assertTrue(third.cantilevers.isEmpty());
        // El mismo objeto, no una copia: es lo que Hibernate usa para escribir la clave foránea.
        assertSame(second, second.cantilevers.getFirst().profile);
    }

    @Test
    void aCantileverOfAMissingProfileFailsSayingWhichOne() throws IOException {
        Path file = dir.resolve("profile-master.xlsx");
        MasterFixtures.writeProfileMaster(file, true);
        addRow(file, "CANTILEVERS", "EP1", "TRACK 1", "1-1.99", 99, 1, "EMT-1");

        MasterFormatException e = assertThrows(MasterFormatException.class,
                () -> reader.read(new MasterWorkbook(file)));
        assertTrue(e.getMessage().contains("EP1 / TRACK 1 / orden 99"), e.getMessage());
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

    /** Añade una fila al final de una hoja de un Excel ya escrito: números como número, el resto como texto. */
    private static void addRow(Path file, String sheetName, Object... values) throws IOException {
        XSSFWorkbook wb;
        try (InputStream in = Files.newInputStream(file)) {
            wb = new XSSFWorkbook(in);
        }
        try (wb; OutputStream out = Files.newOutputStream(file)) {
            Sheet sheet = wb.getSheet(sheetName);
            Row row = sheet.createRow(sheet.getLastRowNum() + 1);
            for (int c = 0; c < values.length; c++) {
                if (values[c] instanceof Number n) {
                    row.createCell(c).setCellValue(n.doubleValue());
                } else {
                    row.createCell(c).setCellValue(String.valueOf(values[c]));
                }
            }
            wb.write(out);
        }
    }

    private ProfileMaster read(boolean withSectioning) {
        Path file = dir.resolve("profile-master.xlsx");
        MasterFixtures.writeProfileMaster(file, withSectioning);
        return reader.read(new MasterWorkbook(file));
    }
}
