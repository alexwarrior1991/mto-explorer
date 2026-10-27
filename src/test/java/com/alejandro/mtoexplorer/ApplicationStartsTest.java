package com.alejandro.mtoexplorer;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Comprueba que la aplicación arranca con todo su andamiaje: PostgreSQL y Redis por Dev Services,
 * Flyway, Hibernate Reactive y gRPC. Si este test falla, el problema es de entorno (¿está Docker
 * arrancado?), no de tu código.
 */
@QuarkusTest
class ApplicationStartsTest {

    @Test
    void isReady() {
        given().get("/q/health/ready").then().statusCode(200);
    }
}
