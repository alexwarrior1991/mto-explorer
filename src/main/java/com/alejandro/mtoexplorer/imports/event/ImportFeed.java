package com.alejandro.mtoexplorer.imports.event;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.operators.multi.processors.SerializedProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * El canal en directo de las importaciones: un {@link Multi} al que se suscribe cada cliente SSE
 * ({@code GET /api/loads/events}) y por el que sale un {@link ImportEvent} por fichero importado.
 *
 * <p>Hasta ahora los {@code Multi} del proyecto eran <b>fríos</b>: cada suscriptor recibe todo
 * desde el principio (el streaming de perfiles hace sus consultas para cada cliente). Este es
 * <b>caliente</b>: los eventos ocurren aunque no haya nadie escuchando, y cada suscriptor recibe
 * los que pasan mientras está suscrito, no los de antes.
 *
 * <p>Lo hace un {@link BroadcastProcessor}: un {@code Multi} en el que se puede emitir a mano
 * ({@code onNext}) y que reparte cada elemento a todos sus suscriptores. Se emite por su versión
 * {@code serialized()}, que ordena las emisiones si llegan desde varios hilos a la vez (el arranque,
 * una petición, el Scheduler…), y los clientes se suscriben al procesador.
 */

@ApplicationScoped
public class ImportFeed {

    private final BroadcastProcessor<ImportEvent> processor = BroadcastProcessor.create();
    private final SerializedProcessor<ImportEvent, ImportEvent> emitter = processor.serialized();

    /**
     * Al terminar cada importación, un evento por fichero. Sin suscriptores, se pierde (es en directo).
     */
    void onImport(@Observes MastersImported imported) {
        imported.results().forEach(result -> emitter.onNext(new ImportEvent(imported.at(), result)));
    }

    /**
     * Los eventos desde ahora. Si un cliente lee despacio, se le descartan los que no da abasto a leer.
     */
    public Multi<ImportEvent> events() {
        return processor.onOverflow().drop();
    }


}
