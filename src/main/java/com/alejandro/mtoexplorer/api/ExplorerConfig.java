package com.alejandro.mtoexplorer.api;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.nio.file.Path;
import java.util.Optional;

/** Propiedades mto.* de application.yml. */
@ConfigMapping(prefix = "mto")
public interface ExplorerConfig {

    /** Carpeta de los maestros. Sin ella la aplicación arranca vacía. */
    Optional<Path> dataDir();

    @WithDefault("lov-master.xlsx")
    String lovFile();

    @WithDefault("profile-master.xlsx")
    String profileFile();

    @WithDefault("true")
    boolean loadOnStartup();

}
