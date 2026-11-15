package com.alejandro.mtoexplorer.repository;

import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.imports.MasterFixturesResource;
import com.alejandro.mtoexplorer.repository.criteria.ProfileSearch;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Un repositorio reactivo probado sin HTTP. Sus métodos devuelven {@code Uni} y necesitan una
 * sesión, que solo existe en un contexto de Vert.x: {@code @RunOnVertxContext} ejecuta el test ahí,
 * y {@link UniAsserter} espera a cada {@code Uni} antes de comprobar su resultado.
 */
@QuarkusTest
@WithTestResource(MasterFixturesResource.class)
class ProfileRepositoryTest {

    @Inject
    ProfileRepository profiles;

    @Test
    @RunOnVertxContext
    void findsAProfileByOrderEvenWithARepeatedProfileId(UniAsserter asserter) {
        asserter.assertThat(() -> Panache.withSession(() -> profiles.findByTrackAndOrder("EP1", "TRACK 1", 3)),
                profile -> {
                    assertEquals("1-1.02", profile.profileId);
                    assertEquals(12110.0, profile.kp);
                });
        asserter.assertThat(() -> Panache.withSession(() -> profiles.findByTrackAndOrder("EP1", "TRACK 1", 99)),
                profile -> assertNull(profile));
    }

    @Test
    @RunOnVertxContext
    void searchFiltersByKpRangeAndSortsByTrackAndOrder(UniAsserter asserter) {
        ProfileSearch search = new ProfileSearch("EP1", null, 12000.0, 12100.0, null);
        asserter.assertThat(() -> Panache.withSession(() -> profiles.search(search).list()),
                (List<Profile> found) -> assertEquals(List.of("TRACK 1/1", "TRACK 1/2", "TRACK 2/1"),
                        found.stream().map(p -> p.track.name + "/" + p.sortOrder).toList()));
        asserter.assertEquals(() -> Panache.withSession(() -> profiles.count(search)), 3L);
    }

    @Test
    @RunOnVertxContext
    void countsProfilesAndToReviewPerExecutionPackage(UniAsserter asserter) {
        asserter.assertEquals(() -> Panache.withSession(() -> profiles.countPerEp()),
                List.of(new EpCount("EP1", 5, 1), new EpCount("EP2", 1, 1)));
    }
}
