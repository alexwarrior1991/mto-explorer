package com.alejandro.mtoexplorer.service;

import com.alejandro.mtoexplorer.repository.CantileverRepository;
import com.alejandro.mtoexplorer.repository.ProfileRepository;
import com.alejandro.mtoexplorer.repository.UnrecognizedValueRepository;
import com.alejandro.mtoexplorer.repository.projection.EpCount;
import com.alejandro.mtoexplorer.service.model.EpQuality;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Un servicio probado sin Quarkus y sin base de datos: sus repositorios son mocks de Mockito que
 * devuelven un {@code Uni} ya resuelto. Lo que se prueba es la lógica del servicio, cruzar tres
 * recuentos por paquete, no las consultas.
 *
 * <p>Se llama al constructor directamente, así que {@code @WithSession} no actúa (es un
 * interceptor de CDI) y no hace falta sesión.
 */
class QualityServiceTest {

    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final CantileverRepository cantilevers = mock(CantileverRepository.class);
    private final UnrecognizedValueRepository unrecognized = mock(UnrecognizedValueRepository.class);
    private final QualityService service = new QualityService(profiles, cantilevers, unrecognized);

    @Test
    void crossesTheThreeCountsPerExecutionPackage() {
        when(profiles.countPerEp()).thenReturn(Uni.createFrom().item(List.of(
                new EpCount("EP2", 10, 1), new EpCount("EP1", 5, 2))));
        when(cantilevers.countPerEp()).thenReturn(Uni.createFrom().item(List.of(
                new EpCount("EP1", 7, 3))));
        when(unrecognized.countPerEp()).thenReturn(Uni.createFrom().item(List.of(
                new EpCount("EP3", 4, 0))));

        List<EpQuality> quality = service.perExecutionPackage().await().indefinitely();

        assertEquals(List.of(
                // Ordenado por paquete, con 0 donde una de las tres consultas no lo trae.
                new EpQuality("EP1", 5, 2, 7, 3, 0),
                new EpQuality("EP2", 10, 1, 0, 0, 0),
                new EpQuality("EP3", 0, 0, 0, 0, 4)), quality);
    }

    @Test
    void noDataIsAnEmptyList() {
        when(profiles.countPerEp()).thenReturn(Uni.createFrom().item(List.of()));
        when(cantilevers.countPerEp()).thenReturn(Uni.createFrom().item(List.of()));
        when(unrecognized.countPerEp()).thenReturn(Uni.createFrom().item(List.of()));

        assertEquals(List.of(), service.perExecutionPackage().await().indefinitely());
    }
}
