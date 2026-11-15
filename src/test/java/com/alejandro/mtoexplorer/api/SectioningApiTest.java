package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.imports.MasterFixturesResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/** El seccionamiento de los maestros de prueba de MasterFixtures. */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class SectioningApiTest {

    @Test
    void listsDisconnectorsWithAndWithoutPole() {
        given().queryParam("ep", "EP1").get("/api/sectioning/disconnectors")
                .then().statusCode(200)
                .body("name", contains("AAA-01", "NS-01"))
                .body("[0].profileId", equalTo("1-1.01"))
                .body("[0].poleKpText", equalTo("12+000"))
                // Con estos dos se pide la ficha del poste: /api/profiles/EP1/TRACK 1/1.
                .body("[0].poleTrack", equalTo("TRACK 1"))
                .body("[0].poleOrder", is(1))
                .body("[1].station", equalTo("SIN ESTACION"))
                .body("[0].normallyOpen", is(false))
                .body("[1].profileId", nullValue())
                .body("[1].kpText", equalTo("-0+250"))
                // NORMALLY_OPEN vacío es "sin dato", no "cerrado".
                .body("[1].normallyOpen", nullValue());
    }

    @Test
    void filtersDisconnectorsByTrackIncludingTheConnectedOne() {
        given().queryParam("ep", "EP1").queryParam("track", "TRACK 1").get("/api/sectioning/disconnectors")
                .then().statusCode(200)
                .body("name", contains("AAA-01", "NS-01"));
        given().queryParam("ep", "EP1").queryParam("station", "AAA").get("/api/sectioning/disconnectors")
                .then().statusCode(200)
                .body("name", contains("AAA-01"));
        given().queryParam("ep", "EP1").queryParam("station", "SIN ESTACION").get("/api/sectioning/disconnectors")
                .then().statusCode(200)
                .body("name", contains("NS-01"));
    }

    @Test
    void sectionInsulatorsBringTheirOwnSwitchesOnly() {
        given().queryParam("ep", "EP1").get("/api/sectioning/section-insulators")
                .then().statusCode(200)
                .body("station", contains("AAA", "BBB"))
                .body("[0].switches.code", contains("W1", "W2"))
                .body("[0].switches[0].tangentText", equalTo("1:12"))
                .body("[0].switches[1].enabled", is(false))
                // El de BBB se llama igual, pero las agujas son del de AAA.
                .body("[1].switches", hasSize(0));
    }

    @Test
    void sectioningNeedsAnExecutionPackage() {
        given().get("/api/sectioning/disconnectors").then().statusCode(400);
    }
}
