package com.alejandro.mtoexplorer.repository.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryFilterTest {

    @Test
    void addsOnlyTheConditionsWithAValueInOrder() {
        QueryFilter filter = QueryFilter.create()
                .eq("ep", "EP1")
                .eq("track", null)
                .where("kp >= :kpFrom", "kpFrom", 12000.0)
                .where("kp <= :kpTo", "kpTo", null)
                .eq("toReview", false);

        assertEquals("ep = :ep and kp >= :kpFrom and toReview = :toReview", filter.hql());
        assertEquals(Map.of("ep", "EP1", "kpFrom", 12000.0, "toReview", false), filter.params());
        assertEquals(List.of("ep", "kpFrom", "toReview"), List.copyOf(filter.params().keySet()));
    }

    @Test
    void aConditionCanUseItsParameterTwice() {
        QueryFilter filter = QueryFilter.create()
                .where("(track = :track or connectedTrack = :track)", "track", "TRACK 1");

        assertEquals("(track = :track or connectedTrack = :track)", filter.hql());
        assertEquals(Map.of("track", "TRACK 1"), filter.params());
    }

    @Test
    void withoutValuesItIsEmpty() {
        QueryFilter filter = QueryFilter.create().eq("master", null).eq("ep", null);

        assertTrue(filter.isEmpty());
        assertEquals("", filter.hql());
        assertTrue(filter.params().isEmpty());
    }

    @Test
    void paramsCannotBeChangedFromOutside() {
        QueryFilter filter = QueryFilter.create().eq("ep", "EP1");

        assertThrows(UnsupportedOperationException.class, () -> filter.params().put("ep", "EP2"));
    }
}
