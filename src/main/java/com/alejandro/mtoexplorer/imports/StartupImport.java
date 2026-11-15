package com.alejandro.mtoexplorer.imports;

import com.alejandro.mtoexplorer.config.ExplorerConfig;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/** Importa los maestros al arrancar, si han cambiado desde la última vez. */
@ApplicationScoped
class StartupImport {

    void onStart(@Observes StartupEvent event, ExplorerConfig config, MasterImportService imports) {
        if (config.loadOnStartup()) {
            imports.importIfChanged();
        }
    }
}
