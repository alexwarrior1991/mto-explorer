package com.alejandro.mtoexplorer.load;

import com.alejandro.mtoexplorer.api.ExplorerConfig;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/** Carga los maestros al arrancar, si han cambiado desde la última vez. */
@ApplicationScoped
class StartupLoader {

    void onStart(@Observes StartupEvent event, ExplorerConfig config, MasterLoader loader){
        if (config.loadOnStartup()){
            loader.reloadIfChanged();
        }
    }
}
