package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.imports.MasterFixturesResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/** La API sobre los maestros de prueba de {@code MasterFixtures}, cargados al arrancar. */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class ExplorerApiTest {

    @Test
    void listsExecutionPackagesWithTheirDates() {
        given().get("/api/eps")
                .then().statusCode(200)
                .body("code", contains("EP1", "EP2"))
                .body("[0].initialPackage", is(true))
                .body("[0].startDate", equalTo("2020-01-15"))
                .body("[1].startDate", nullValue());
    }

    @Test
    void listsTracksSplittingTheirStations() {
        given().get("/api/eps/EP1/tracks")
                .then().statusCode(200)
                .body("name", contains("TRACK 1", "TRACK 2"))
                .body("[0].stations", contains("AAA", "BBB"));
    }

    @Test
    void overviewCountsTracksAndProfilesPerPackage() {
        given().get("/api/eps/overview")
                .then().statusCode(200)
                .body("code", contains("EP1", "EP2"))
                .body("tracks", contains(2, 1))
                .body("profiles", contains(5, 1));
    }

    @Test
    void unknownExecutionPackageIs404() {
        given().get("/api/eps/EP99/tracks").then().statusCode(404);
    }

    @Test
    void searchesProfilesByTrackAndKpRangeInPlanNotation() {
        given().queryParam("ep", "EP1").queryParam("track", "TRACK 1")
                .queryParam("kpFrom", "12+050").queryParam("kpTo", "12+200")
                .get("/api/profiles")
                .then().statusCode(200)
                .body("total", is(2))
                .body("items.order", contains(2, 3))
                .body("items[0].kpText", equalTo("12+054.5"));
    }

    @Test
    void filtersProfilesPendingReview() {
        given().queryParam("ep", "EP1").queryParam("toReview", true)
                .get("/api/profiles")
                .then().statusCode(200)
                .body("items.profileId", contains("1-1.02"))
                .body("items[0].order", is(2));
    }

    @Test
    void searchWithoutExecutionPackageIs400() {
        given().get("/api/profiles").then().statusCode(400);
    }

    @Test
    void badKpIs400WithAProblem() {
        given().queryParam("ep", "EP1").queryParam("kpFrom", "doce")
                .get("/api/profiles")
                .then().statusCode(400)
                .contentType("application/problem+json")
                .body("detail", containsString("doce"));
    }

    @Test
    void profileDetailBringsItsCantileversInSlotOrder() {
        // Dos perfiles comparten PROFILE_ID 1-1.02: el orden es lo que los distingue.
        given().get("/api/profiles/EP1/TRACK 1/2")
                .then().statusCode(200)
                .body("profileId", equalTo("1-1.02"))
                .body("cantilevers.slot", contains(1, 2))
                .body("cantilevers[0].type", equalTo("EMT-TT"))
                .body("cantilevers[0].stagger", is(-70.0f));
        given().get("/api/profiles/EP1/TRACK 1/1")
                .then().statusCode(200)
                // El seccionador AAA-01 cuelga de este poste (Profile.disconnectors).
                .body("disconnectors.name", contains("AAA-01"))
                .body("disconnectors[0].function", equalTo("LoadB/NS"));
        given().get("/api/profiles/EP1/TRACK 1/3")
                .then().statusCode(200)
                .body("profileId", equalTo("1-1.02"))
                .body("cantilevers", hasSize(0));
    }

    @Test
    void searchesLovsByFamilyAndText() {
        given().queryParam("entity", "Sectioning").get("/api/lovs")
                .then().statusCode(200)
                .body("items.code", contains("A/S", "S/A"))
                .body("items[0].eps", contains("EP1", "EP2"));
        given().queryParam("q", "heb").get("/api/lovs")
                .then().statusCode(200)
                .body("items.code", contains("HEB-200"));
    }

    @Test
    void countsLovFamilies() {
        given().get("/api/lovs/entities")
                .then().statusCode(200)
                .body("entity", contains("PoleType", "Sectioning"))
                .body("[1].codes", is(2))
                .body("[1].toReview", is(1));
    }

    @Test
    void qualityCountsWhatIsLeftToReviewPerPackage() {
        given().get("/api/quality")
                .then().statusCode(200)
                .body("ep", contains("EP1", "EP2"))
                .body("[0].profiles", is(5))
                .body("[0].profilesToReview", is(1))
                .body("[0].cantilevers", is(3))
                .body("[0].cantileversToReview", is(1))
                // Uno del maestro de perfiles y otro del de LOVs.
                .body("[0].unrecognized", is(2))
                .body("[1].unrecognized", is(1));
    }

    @Test
    void listsUnrecognizedValuesOfOneMaster() {
        given().queryParam("master", "lov").get("/api/quality/unrecognized")
                .then().statusCode(200)
                .body("value", contains("AnXX"));
    }

    @Test
    void reloadsOnlyWhatChangedUnlessForced() {
        given().queryParam("force", false).post("/api/loads")
                .then().statusCode(200)
                .body("status", contains("UNCHANGED", "UNCHANGED"));
        given().post("/api/loads")
                .then().statusCode(200)
                .body("status", contains("LOADED", "LOADED"));
        given().get("/api/eps").then().body("code", contains("EP1", "EP2"));
    }
}
