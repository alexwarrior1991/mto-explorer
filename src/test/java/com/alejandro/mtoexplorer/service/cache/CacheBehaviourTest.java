package com.alejandro.mtoexplorer.service.cache;

import com.alejandro.mtoexplorer.imports.FileImportResult;
import com.alejandro.mtoexplorer.imports.FileImportResult.Status;
import com.alejandro.mtoexplorer.imports.MasterFixturesResource;
import com.alejandro.mtoexplorer.imports.MasterImportService;
import com.alejandro.mtoexplorer.imports.event.MastersImported;
import com.alejandro.mtoexplorer.repository.ExecutionPackageRepository;
import com.alejandro.mtoexplorer.repository.ProfileRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * La caché, vista desde fuera. Para saber si una respuesta sale de la caché o de la base de datos,
 * se cambia la base de datos por debajo, sin importar (así no se vacía nada): si la respuesta no
 * cambia, sale de Redis.
 */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class CacheBehaviourTest {

    @Inject
    ExecutionPackageRepository executionPackages;

    @Inject
    ProfileRepository profiles;

    @Inject
    CacheInvalidator invalidator;

    @Inject
    MasterImportService imports;

    @Inject
    Event<MastersImported> imported;

    /**
     * Deja la base de datos y la caché como estaban: la importación vuelve a escribir los maestros y vacía.
     */
    @AfterEach
    void restore() {
        imports.importAll();
    }

    @Test
    void overviewIsServedFromCacheUntilItIsInvalidated() throws Throwable {
        overviewNameOfEp1Is("Paquete uno");

        renameEp1("Renombrado por debajo");
        // La base de datos ha cambiado, la caché no.
        overviewNameOfEp1Is("Paquete uno");

        invalidator.invalidate(List.of(CacheNames.EP_OVERVIEW));
        overviewNameOfEp1Is("Renombrado por debajo");
    }

    @Test
    void importingAMasterInvalidatesOnlyTheCachesThatDependOnIt() throws Throwable {
        overviewNameOfEp1Is("Paquete uno");
        ep1ProfilesToReviewAre(1);

        renameEp1("Renombrado por debajo");
        markEveryProfileOfEp1ToReview();
        overviewNameOfEp1Is("Paquete uno");
        ep1ProfilesToReviewAre(1);

        // El de LOVs: vacía la calidad (depende de los dos maestros) pero no el resumen de paquetes.
        imported.fire(eventFor("LOV"));
        ep1ProfilesToReviewAre(5);
        overviewNameOfEp1Is("Paquete uno");

        // El de perfiles: ahora sí el resumen.
        imported.fire(eventFor("PROFILE"));
        overviewNameOfEp1Is("Renombrado por debajo");
    }

    @Test
    void qualityIsCachedPerExecutionPackage() throws Throwable {
        ep1ProfilesToReviewAre(1);
        given().get("/api/quality/EP2").then().statusCode(200).body("profilesToReview", is(1));

        markEveryProfileOfEp1ToReview();
        invalidator.invalidate(List.of(CacheNames.QUALITY));
        // quality-by-ep tiene su propia entrada para EP1, que sigue ahí.
        ep1ProfilesToReviewAre(1);

        invalidator.invalidate(List.of(CacheNames.QUALITY_BY_EP));
        ep1ProfilesToReviewAre(5);
    }

    @Test
    void anUnknownExecutionPackageIsNotFoundAndTheFailureIsNotCached() {
        given().get("/api/quality/EP99").then().statusCode(404);
        given().get("/api/quality/EP99").then().statusCode(404);
    }

    private static void overviewNameOfEp1Is(String name) {
        given().get("/api/eps/overview").then().statusCode(200).body("[0].name", equalTo(name));
    }

    private static void ep1ProfilesToReviewAre(int count) {
        given().get("/api/quality/EP1").then().statusCode(200).body("profilesToReview", is(count));
    }

    private void renameEp1(String name) throws Throwable {
        inTransaction(() -> executionPackages.update("name = ?1 where code = ?2", name, "EP1"));
    }

    private void markEveryProfileOfEp1ToReview() throws Throwable {
        inTransaction(() -> profiles.update(
                "toReview = true where track.id in (select t.id from Track t where t.executionPackage.code = ?1)",
                "EP1"));
    }

    /**
     * Un cambio en la base de datos desde el hilo del test: entrar al contexto de Vert.x y esperar.
     */
    private static void inTransaction(Supplier<Uni<Integer>> change) throws Throwable {
        VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(change));
    }

    /**
     * Un evento como el que publicaría el importador si hubiera cargado ese maestro.
     */
    private static MastersImported eventFor(String master) {
        return new MastersImported(Instant.now(),
                List.of(new FileImportResult(master, master + ".xlsx", Status.LOADED, 1, 1, null)));
    }
}
