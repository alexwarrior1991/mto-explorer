package com.alejandro.mtoexplorer.imports.event;

import com.alejandro.mtoexplorer.imports.FileImportResult.Status;
import com.alejandro.mtoexplorer.imports.MasterFixturesResource;
import com.alejandro.mtoexplorer.imports.MasterImportService;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El {@code Multi} caliente de las importaciones, probado con {@link AssertSubscriber}, el
 * suscriptor de pruebas de Mutiny: pide elementos, los guarda y deja esperar a que lleguen.
 */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class ImportFeedTest {

    @Inject
    ImportFeed feed;

    @Inject
    MasterImportService imports;

    @Test
    void everySubscriberReceivesOneEventPerImportedFile() {
        AssertSubscriber<ImportEvent> first = feed.events().subscribe().withSubscriber(AssertSubscriber.create(10));
        AssertSubscriber<ImportEvent> second = feed.events().subscribe().withSubscriber(AssertSubscriber.create(10));

        imports.importAll();

        for (AssertSubscriber<ImportEvent> subscriber : List.of(first, second)) {
            List<ImportEvent> events = subscriber.awaitItems(2).getItems();
            assertEquals(List.of("LOV", "PROFILE"), events.stream().map(e -> e.result().master()).toList());
            assertEquals(List.of(Status.LOADED, Status.LOADED),
                    events.stream().map(e -> e.result().status()).toList());
            // Los dos ficheros de una importación llevan la misma hora: la de la importación.
            assertEquals(events.get(0).at(), events.get(1).at());
            subscriber.cancel();
        }
    }

    @Test
    void aLateSubscriberDoesNotReceiveWhatHappenedBefore() {
        imports.importIfChanged();

        AssertSubscriber<ImportEvent> late = feed.events().subscribe().withSubscriber(AssertSubscriber.create(10));
        imports.importIfChanged();

        List<ImportEvent> events = late.awaitItems(2).getItems();
        assertEquals(2, events.size());
        assertEquals(List.of(Status.UNCHANGED, Status.UNCHANGED),
                events.stream().map(e -> e.result().status()).toList());
        late.cancel();
    }
}