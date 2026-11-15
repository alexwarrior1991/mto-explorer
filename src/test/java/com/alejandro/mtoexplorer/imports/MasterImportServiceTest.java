package com.alejandro.mtoexplorer.imports;

import com.alejandro.mtoexplorer.config.ExplorerConfig;
import com.alejandro.mtoexplorer.imports.FileImportResult.Status;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El orquestador de la importación, llamado directamente (sin HTTP). Es bloqueante, y el hilo
 * del test puede bloquearse: no hace falta nada especial para llamarlo.
 */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class MasterImportServiceTest {

    @Inject
    MasterImportService imports;

    @Inject
    ExplorerConfig config;

    @Test
    void importsOnlyWhatChangedSinceTheLastLoad() throws IOException {
        // Ya se importaron al arrancar: nada ha cambiado.
        assertEquals(List.of(Status.UNCHANGED, Status.UNCHANGED), statuses(imports.importIfChanged()));

        // Un maestro con otra fecha de modificación es un maestro nuevo.
        Path lovFile = config.dataDir().orElseThrow().resolve(config.lovFile());
        Files.setLastModifiedTime(lovFile, FileTime.from(Instant.now().plusSeconds(3600)));

        assertEquals(List.of(Status.LOADED, Status.UNCHANGED), statuses(imports.importIfChanged()));
        assertEquals(List.of(Status.UNCHANGED, Status.UNCHANGED), statuses(imports.importIfChanged()));
    }

    @Test
    void importAllLoadsBothAndCountsEveryRow() {
        List<FileImportResult> results = imports.importAll();

        assertEquals(List.of(Status.LOADED, Status.LOADED), statuses(results));
        assertEquals("lov-master.xlsx", results.get(0).file());
        assertEquals(4, results.get(0).rows());
        assertEquals("profile-master.xlsx", results.get(1).file());
        assertEquals(25, results.get(1).rows());
    }

    private static List<Status> statuses(List<FileImportResult> results) {
        return results.stream().map(FileImportResult::status).toList();
    }
}
