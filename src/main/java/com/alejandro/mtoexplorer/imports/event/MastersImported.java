package com.alejandro.mtoexplorer.imports.event;

import com.alejandro.mtoexplorer.imports.FileImportResult;

import java.time.Instant;
import java.util.List;

/**
 * Evento CDI: ha terminado una importación (al arrancar, por la API o, en la fase 4, por el
 * Scheduler). Lo publica {@code MasterImportService} y lo escuchan quienes tienen que enterarse
 * (la caché, el canal SSE) sin que el importador sepa quiénes son.
 *
 * @param at      cuándo terminó
 * @param results lo que pasó con cada fichero, en el orden en que se importaron
 */
public record MastersImported(Instant at, List<FileImportResult> results) {

    /** Los maestros (LOV, PROFILE) que se han cargado de verdad: los que han cambiado algo. */
    public List<String> loadedMasters(){
        return results.stream().filter(FileImportResult::loaded).map(FileImportResult::master).toList();
    }
}
