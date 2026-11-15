package com.alejandro.mtoexplorer.imports;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;

/** Una carpeta de datos que no tiene los maestros: la aplicación arranca vacía y lo dice. */
@QuarkusTest
@TestProfile(MissingDataDirTest.EmptyDataDir.class)
class MissingDataDirTest {

    public static class EmptyDataDir implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("mto.data-dir", "/carpeta/que/no/existe");
        }
    }

    @Test
    void startsEmptyAndReportsBothMastersAsMissing() {
        given().get("/api/eps").then().statusCode(200).body("$", empty());

        given().queryParam("force", false).post("/api/loads")
                .then().statusCode(200)
                .body("file", contains("lov-master.xlsx", "profile-master.xlsx"))
                .body("status", contains("MISSING", "MISSING"))
                .body("detail", everyItem(containsString("/carpeta/que/no/existe")));
    }
}
