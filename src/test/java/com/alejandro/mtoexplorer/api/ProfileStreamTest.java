package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.imports.MasterFixturesResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El streaming de perfiles (un {@code Multi}). Con páginas de 2, los 5 perfiles de EP1 salen de
 * tres consultas (2 + 2 + 1) más una vacía que cierra el flujo: así se ve que el Multi encadena
 * páginas sin que el cliente lo note.
 */
@QuarkusTest
@TestProfile(ProfileStreamTest.SmallPages.class)
@WithTestResource(MasterFixturesResource.class)
class ProfileStreamTest {

    public static class SmallPages implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("mto.stream-page-size", "2");
        }
    }

    @Test
    void streamsEveryProfileAsNdjsonAcrossPages() {
        List<JsonPath> lines = ndjson(given().queryParam("ep", "EP1").get("/api/profiles/stream")
                .then().statusCode(200)
                .contentType("application/x-ndjson")
                .extract().asString());

        assertEquals(5, lines.size());
        // El mismo orden que la búsqueda paginada: por vía y orden.
        assertEquals(List.of("TRACK 1", "TRACK 1", "TRACK 1", "TRACK 1", "TRACK 2"),
                lines.stream().map(l -> l.getString("track")).toList());
        assertEquals(List.of(1, 2, 3, 4, 1), lines.stream().map(l -> l.getInt("order")).toList());
        assertEquals("12+054.5", lines.get(1).getString("kpText"));
    }

    @Test
    void streamAppliesTheSameFiltersAsTheSearch() {
        List<JsonPath> lines = ndjson(given().queryParam("ep", "EP1").queryParam("track", "TRACK 1")
                .queryParam("kpFrom", "12+050").queryParam("kpTo", "12+200")
                .get("/api/profiles/stream")
                .then().statusCode(200).extract().asString());

        assertEquals(List.of(2, 3), lines.stream().map(l -> l.getInt("order")).toList());
    }

    @Test
    void streamOfAnEmptySearchIsEmpty() {
        String body = given().queryParam("ep", "EP99").get("/api/profiles/stream")
                .then().statusCode(200).extract().asString();
        assertTrue(body.isBlank());
    }

    @Test
    void streamValidatesLikeTheSearch() {
        given().get("/api/profiles/stream").then().statusCode(400);
    }

    private static List<JsonPath> ndjson(String body) {
        return Arrays.stream(body.split("\n")).filter(l -> !l.isBlank()).map(JsonPath::from).toList();
    }
}
