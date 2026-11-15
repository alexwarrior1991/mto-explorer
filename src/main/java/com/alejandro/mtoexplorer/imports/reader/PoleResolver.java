package com.alejandro.mtoexplorer.imports.reader;

import com.alejandro.mtoexplorer.entity.Profile;

import java.util.List;
import java.util.Optional;

/**
 * Qué perfil es el poste de un seccionador. El maestro lo dice con PROFILE_ID, que no es único
 * en un paquete (una vía de dos tramos lo repite; un poste de enlace está en las dos vías que se
 * cruzan), así que se desempata como dice el README de mto-configuration:
 * <ol>
 *   <li>Si solo hay un perfil con ese PROFILE_ID en el paquete, es ese.
 *   <li>Si no, el que está en el KP_POSTE.
 *   <li>Si sigue habiendo varios (o ninguno en ese KP), el de la vía del seccionador.
 * </ol>
 * Con los maestros de hoy, los 646 seccionadores con poste se resuelven.
 */
final class PoleResolver {

    /** Dos KP iguales con decimales: el maestro los escribe con dos. */
    private static final double KP_TOLERANCE = 0.01;

    private PoleResolver() {
    }

    /**
     * @param candidates los perfiles del paquete con el PROFILE_ID del seccionador
     * @param poleKp     KP_POSTE, o null
     * @param trackName  VIA del seccionador, o null
     * @return el poste, o vacío si no hay candidatos o no se puede desempatar
     */
    static Optional<Profile> resolve(List<Profile> candidates, Double poleKp, String trackName) {
        if (candidates.size() <= 1) {
            return candidates.stream().findFirst();
        }
        List<Profile> atKp = candidates.stream()
                .filter(p -> poleKp != null && p.kp != null && Math.abs(p.kp - poleKp) < KP_TOLERANCE)
                .toList();
        if (atKp.size() == 1) {
            return Optional.of(atKp.getFirst());
        }
        List<Profile> onTrack = (atKp.isEmpty() ? candidates : atKp).stream()
                .filter(p -> p.track.name.equals(trackName))
                .toList();
        return onTrack.size() == 1 ? Optional.of(onTrack.getFirst()) : Optional.empty();
    }
}
