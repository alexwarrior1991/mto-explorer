package com.alejandro.mtoexplorer.imports;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MasterFixturesResource implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        Path dir;
        try {
            dir = Files.createTempDirectory("mto-explorer-test");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        MasterFixtures.writeAll(dir);
        return Map.of("mto.data-dir", dir.toString());
    }

    @Override
    public void stop() {

    }
}
