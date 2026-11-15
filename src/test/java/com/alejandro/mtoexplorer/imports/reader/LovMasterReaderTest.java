package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Lov;
import com.alejandro.mtoexplorer.imports.MasterFixtures;
import com.alejandro.mtoexplorer.imports.excel.MasterWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LovMasterReaderTest {

    @TempDir
    Path dir;

    @Test
    void readsLovsAndTheirUnrecognizedSheet() {
        Path file = dir.resolve("lov-master.xlsx");
        MasterFixtures.writeLovMaster(file);

        LovMaster master = new LovMasterReader().read(new MasterWorkbook(file));

        assertEquals(3, master.lovs().size());
        assertEquals(1, master.unrecognized().size());
        assertEquals(4, master.rows());

        Lov first = master.lovs().getFirst();
        assertEquals("Sectioning", first.entity);
        assertEquals("A/S", first.code);
        assertNull(first.descriptionEs);
        assertEquals("EP1, EP2", first.eps);
        assertEquals(12, first.usesInTracks);
        assertTrue(first.enabled);
        assertFalse(first.toReview);

        assertEquals(LovMasterReader.MASTER, master.unrecognized().getFirst().master);
        assertEquals("AnXX", master.unrecognized().getFirst().value);
    }
}
