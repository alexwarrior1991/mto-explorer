package com.alejandro.mtoexplorer.service.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Qué cachés se vacían al importar cada maestro. */
class CacheNamesTest {

    @Test
    void eachMasterInvalidatesItsCaches() {
        assertEquals(List.of(CacheNames.LOV_FAMILIES, CacheNames.QUALITY, CacheNames.QUALITY_BY_EP),
                CacheNames.forMasters(List.of("LOV")));
        assertEquals(List.of(CacheNames.EP_OVERVIEW, CacheNames.QUALITY, CacheNames.QUALITY_BY_EP),
                CacheNames.forMasters(List.of("PROFILE")));
    }

    @Test
    void bothMastersInvalidateEveryCacheOnce() {
        List<String> both = CacheNames.forMasters(List.of("LOV", "PROFILE"));
        assertEquals(4, both.size());
        assertTrue(both.containsAll(CacheNames.ALL));
    }

    @Test
    void nothingLoadedInvalidatesNothing() {
        assertTrue(CacheNames.forMasters(List.of()).isEmpty());
    }
}
