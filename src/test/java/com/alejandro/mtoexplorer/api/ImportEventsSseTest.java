package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.imports.MasterFixturesResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El canal SSE por HTTP, como lo usaría un cliente: abre la conexión, espera al evento "connected",
 * se importa desde otra petición y llegan los eventos "import". Se lee con el HttpClient de Java,
 * línea a línea. En SSE cada evento son varias líneas ("event:import", "data:{...}") y una vacía.
 */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class ImportEventsSseTest {

    @TestHTTPResource("/api/loads/events")
    URI events;

    /**
     * Un evento SSE ya leído: su nombre y su data.
     */
    private record SseEvent(String name, String data) {
    }

    @Test
    void anImportRequestIsAnnouncedToTheOpenConnection() throws Exception {
        // Sin try-with-resources: close() espera a que terminen las peticiones en curso, y un SSE no
        // termina nunca. shutdownNow() las corta.
        HttpClient client = HttpClient.newHttpClient();
        try {
            HttpRequest request = HttpRequest.newBuilder(events).header("Accept", "text/event-stream").build();
            HttpResponse<Stream<String>> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                    .get(10, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/event-stream"));
            BlockingQueue<SseEvent> received = readEventsInBackground(response.body());

            SseEvent connected = received.poll(10, TimeUnit.SECONDS);
            assertNotNull(connected, "no ha llegado el evento connected");
            assertEquals("connected", connected.name());

            // Ya está suscrito al canal: lo que se importe ahora tiene que llegar.
            given().post("/api/loads").then().statusCode(200);

            List<JsonPath> imports = new ArrayList<>();
            while (imports.size() < 2) {
                SseEvent event = received.poll(10, TimeUnit.SECONDS);
                assertNotNull(event, "no han llegado los dos eventos import");
                assertEquals("import", event.name());
                imports.add(JsonPath.from(event.data()));
            }
            assertEquals(List.of("lov-master.xlsx", "profile-master.xlsx"),
                    imports.stream().map(e -> e.getString("file")).toList());
            assertEquals(List.of("LOADED", "LOADED"), imports.stream().map(e -> e.getString("status")).toList());
            assertEquals(25, imports.get(1).getInt("rows"));
        } finally {
            client.shutdownNow();
        }
    }

    /**
     * Lee las líneas en otro hilo y deja en la cola cada evento completo (los comentarios ":" no).
     */
    private static BlockingQueue<SseEvent> readEventsInBackground(Stream<String> lines) {
        BlockingQueue<SseEvent> queue = new LinkedBlockingQueue<>();
        Thread.ofVirtual().start(() -> {
            try {
                readEvents(lines, queue);
            } catch (UncheckedIOException closed) {
                // Al acabar el test se corta la conexión (shutdownNow): es lo esperado.
            }
        });
        return queue;
    }

    /**
     * Un evento SSE son varias líneas ("event:…", "data:…") y una vacía que lo cierra.
     */
    private static void readEvents(Stream<String> lines, BlockingQueue<SseEvent> queue) {
        String name = null;
        StringBuilder data = new StringBuilder();
        for (String line : (Iterable<String>) lines::iterator) {
            if (line.isEmpty()) {
                if (name != null || !data.isEmpty()) {
                    queue.add(new SseEvent(name, data.toString()));
                }
                name = null;
                data.setLength(0);
            } else if (line.startsWith("event:")) {
                name = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
                data.append(line.substring("data:".length()).trim());
            }
        }
    }
}
