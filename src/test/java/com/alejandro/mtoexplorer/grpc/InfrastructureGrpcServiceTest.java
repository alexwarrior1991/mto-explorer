package com.alejandro.mtoexplorer.grpc;

import com.alejandro.mtoexplorer.grpc.v1.CantileverInfo;
import com.alejandro.mtoexplorer.grpc.v1.ExecutionPackageInfo;
import com.alejandro.mtoexplorer.grpc.v1.GetExecutionPackageRequest;
import com.alejandro.mtoexplorer.grpc.v1.ImportNotice;
import com.alejandro.mtoexplorer.grpc.v1.ImportStatus;
import com.alejandro.mtoexplorer.grpc.v1.Infrastructure;
import com.alejandro.mtoexplorer.grpc.v1.ProfileInfo;
import com.alejandro.mtoexplorer.grpc.v1.ProfileKey;
import com.alejandro.mtoexplorer.grpc.v1.ProfileLookup;
import com.alejandro.mtoexplorer.grpc.v1.ProfileQuery;
import com.alejandro.mtoexplorer.grpc.v1.ProfileSummary;
import com.alejandro.mtoexplorer.grpc.v1.WatchImportsRequest;
import com.alejandro.mtoexplorer.imports.MasterFixturesResource;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El servicio gRPC de verdad, por la red: {@code @GrpcClient} inyecta el cliente de Mutiny
 * (la misma interfaz {@link Infrastructure} que implementa el servidor) y llama al servidor del
 * test, que comparte puerto con HTTP.
 */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class InfrastructureGrpcServiceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @GrpcClient("infrastructure")
    Infrastructure client;

    @Test
    void getExecutionPackageReturnsItsCounts() {
        ExecutionPackageInfo ep = client.getExecutionPackage(GetExecutionPackageRequest.newBuilder().setCode("EP1").build())
                .await().atMost(TIMEOUT);

        assertEquals("Paquete uno", ep.getName());
        assertEquals(2, ep.getTracks());
        assertEquals(5, ep.getProfiles());
    }

    @Test
    void anUnknownPackageIsNotFound() {
        assertStatus(Status.Code.NOT_FOUND, () -> client
                .getExecutionPackage(GetExecutionPackageRequest.newBuilder().setCode("EP9").build())
                .await().atMost(TIMEOUT));
    }

    @Test
    void getProfileReturnsItsCantileversAndDisconnectors() {
        ProfileInfo profile = client.getProfile(key("EP1", "TRACK 1", 2)).await().atMost(TIMEOUT);

        assertEquals("1-1.02", profile.getSummary().getProfileId());
        assertEquals("12+054.5", profile.getSummary().getKpText());
        assertTrue(profile.getSummary().getToReview());
        assertEquals(List.of(1, 2), profile.getCantileversList().stream().map(CantileverInfo::getSlot).toList());
        // Un optional sin valor no viene: hasX() es false (y getX() daría 0, no null).
        assertFalse(profile.getCantilevers(1).hasCwHeight());

        ProfileInfo withPole = client.getProfile(key("EP1", "TRACK 1", 1)).await().atMost(TIMEOUT);
        assertEquals(List.of("AAA-01"), withPole.getDisconnectorsList());
    }

    @Test
    void anUnknownProfileIsNotFound() {
        assertStatus(Status.Code.NOT_FOUND, () -> client.getProfile(key("EP1", "TRACK 1", 99))
                .await().atMost(TIMEOUT));
    }

    @Test
    void aMissingRequiredFieldIsAnInvalidArgument() {
        // En proto3 lo que no se manda llega como "": el servidor lo rechaza como hace @NotBlank en REST.
        assertStatus(Status.Code.INVALID_ARGUMENT, () -> client
                .getExecutionPackage(GetExecutionPackageRequest.getDefaultInstance())
                .await().atMost(TIMEOUT));
        assertStatus(Status.Code.INVALID_ARGUMENT, () -> client
                .getProfile(ProfileKey.newBuilder().setEp("EP1").setOrder(1).build())
                .await().atMost(TIMEOUT));
    }

    @Test
    void streamProfilesSendsEveryProfileInOrder() {
        List<ProfileSummary> profiles = client.streamProfiles(ProfileQuery.newBuilder().setEp("EP1").build())
                .collect().asList().await().atMost(TIMEOUT);

        assertEquals(List.of("TRACK 1/1", "TRACK 1/2", "TRACK 1/3", "TRACK 1/4", "TRACK 2/1"),
                profiles.stream().map(p -> p.getTrack() + "/" + p.getOrder()).toList());
    }

    @Test
    void streamProfilesFiltersByKp() {
        List<ProfileSummary> profiles = client.streamProfiles(ProfileQuery.newBuilder()
                        .setEp("EP1").setTrack("TRACK 1").setKpFrom("12+050").setKpTo("12+200").build())
                .collect().asList().await().atMost(TIMEOUT);

        assertEquals(List.of(2, 3), profiles.stream().map(ProfileSummary::getOrder).toList());
    }

    @Test
    void anInvalidQueryIsAnInvalidArgument() {
        assertStatus(Status.Code.INVALID_ARGUMENT, () -> client
                .streamProfiles(ProfileQuery.newBuilder().setEp("EP1").setKpFrom("12+ABC").build())
                .collect().asList().await().atMost(TIMEOUT));
        assertStatus(Status.Code.INVALID_ARGUMENT, () -> client
                .streamProfiles(ProfileQuery.getDefaultInstance())
                .collect().asList().await().atMost(TIMEOUT));
    }

    @Test
    void lookupProfilesAnswersEveryKeyInOrderAndAMissingOneDoesNotStopTheStream() {
        Multi<ProfileKey> keys = Multi.createFrom().items(
                key("EP1", "TRACK 1", 1), key("EP1", "TRACK 1", 99), key("EP1", "TRACK 2", 1));

        List<ProfileLookup> answers = client.lookupProfiles(keys).collect().asList().await().atMost(TIMEOUT);

        assertEquals(List.of(1, 99, 1), answers.stream().map(a -> a.getKey().getOrder()).toList());
        assertEquals("1-1.01", answers.get(0).getProfile().getSummary().getProfileId());
        assertEquals(ProfileLookup.ResultCase.ERROR, answers.get(1).getResultCase());
        assertEquals("1-2.01", answers.get(2).getProfile().getSummary().getProfileId());
    }

    @Test
    void watchImportsAnnouncesEveryImportedFile() {
        AssertSubscriber<ImportNotice> notices = client.watchImports(WatchImportsRequest.getDefaultInstance())
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        try {
            // No hay un "conectado" como en el SSE: el cliente no sabe cuándo se ha suscrito el
            // servidor al canal. Se importa hasta que llegan avisos (casi siempre, a la primera).
            for (int attempt = 0; attempt < 10 && notices.getItems().size() < 2; attempt++) {
                given().post("/api/loads").then().statusCode(200);
                try {
                    notices.awaitItems(2, Duration.ofSeconds(1));
                } catch (AssertionError notYet) {
                    // el servidor aún no estaba suscrito: otra vez
                }
            }
            List<ImportNotice> received = notices.getItems();
            assertTrue(received.size() >= 2, "no han llegado los avisos de la importación");
            assertEquals(List.of("lov-master.xlsx", "profile-master.xlsx"),
                    received.subList(0, 2).stream().map(ImportNotice::getFile).sorted().toList());
            assertTrue(received.stream().allMatch(n -> n.getStatus() == ImportStatus.IMPORT_STATUS_LOADED));
            assertTrue(received.stream().allMatch(ImportNotice::hasAt));
        } finally {
            notices.cancel();
        }
    }

    private static ProfileKey key(String ep, String track, int order) {
        return ProfileKey.newBuilder().setEp(ep).setTrack(track).setOrder(order).build();
    }

    /** El cliente recibe el error como un StatusRuntimeException con su código. */
    private static void assertStatus(Status.Code expected, Supplier<?> call) {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, call::get);
        assertEquals(expected, e.getStatus().getCode(), e.getStatus().getDescription());
    }
}
