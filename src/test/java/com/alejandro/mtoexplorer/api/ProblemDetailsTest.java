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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/** La forma de los errores: application/problem+json (RFC 9457) con un type por clase de error. */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class ProblemDetailsTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Test
    void unknownResourceIsNotFound() {
        given().get("/api/eps/EP99/tracks")
                .then().statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/not-found"))
                .body("title", equalTo("Recurso no encontrado"))
                .body("status", equalTo(404))
                .body("detail", equalTo("No existe el paquete EP99"))
                .body("instance", equalTo("/api/eps/EP99/tracks"))
                .body("timestamp", notNullValue())
                // Solo los errores de validación llevan violations.
                .body("violations", nullValue());
    }

    @Test
    void unknownProfileSaysWhichOne() {
        given().get("/api/profiles/EP1/TRACK 1/99")
                .then().statusCode(404)
                .body("detail", equalTo("No existe el perfil EP1 / TRACK 1 / 99"));
    }

    @Test
    void validationListsEveryFieldThatFails() {
        given().queryParam("size", 1000).queryParam("page", -1).get("/api/profiles")
                .then().statusCode(400)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/validation"))
                .body("detail", equalTo("3 parámetro(s) no válido(s)"))
                .body("violations", hasSize(3))
                .body("violations.field", contains("ep", "page", "size"));
    }

    @Test
    void badKpIsAnInvalidParameter() {
        given().queryParam("ep", "EP1").queryParam("kpFrom", "doce").get("/api/profiles")
                .then().statusCode(400)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/invalid-parameter"))
                .body("detail", containsString("doce"));
    }

    @Test
    void unknownRouteKeepsItsHttpStatus() {
        given().get("/api/no-existe")
                .then().statusCode(404)
                .contentType(PROBLEM_JSON)
                .body("type", equalTo("/problems/http"))
                .body("title", equalTo("Not Found"));
    }

    @Test
    void wrongMethodKeepsItsHttpStatus() {
        given().delete("/api/eps")
                .then().statusCode(405)
                .contentType(PROBLEM_JSON)
                .body("title", equalTo("Method Not Allowed"));
    }
}
