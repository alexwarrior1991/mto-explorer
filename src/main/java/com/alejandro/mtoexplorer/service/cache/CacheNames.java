package com.alejandro.mtoexplorer.service.cache;

import com.alejandro.mtoexplorer.imports.reader.LovMasterReader;
import com.alejandro.mtoexplorer.imports.reader.ProfileMasterReader;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Las cachés de la aplicación y de qué maestro depende cada una.
 *
 * <p>Qué se cachea: solo resultados agregados y en datos planos (records sin entidades). Las cachés
 * van en Redis, que guarda JSON; una entidad de Hibernate, con relaciones LAZY y proxies, no se puede
 * convertir a JSON y volver sin perder cosas o fallar. Y los listados de entidades son consultas
 * sencillas que PostgreSQL resuelve igual de rápido.
 *
 * <p>Los datos solo cambian al importar un maestro, así que una entrada vale hasta que se importa su
 * maestro: entonces {@link CacheInvalidator} vacía las cachés que dependen de él.
 */
public final class CacheNames {

    /**
     * {@code ExecutionPackageService.overview()}: vías y perfiles por paquete.
     */
    public static final String EP_OVERVIEW = "ep-overview";
    /**
     * {@code QualityService.perExecutionPackage()}: la calidad de todos los paquetes.
     */
    public static final String QUALITY = "quality";
    /**
     * {@code QualityService.ofExecutionPackage(ep)}: la de un paquete; la clave es el código del paquete.
     */
    public static final String QUALITY_BY_EP = "quality-by-ep";
    /**
     * {@code LovService.families()}: códigos y pendientes de revisar por familia de LOVs.
     */
    public static final String LOV_FAMILIES = "lov-families";

    /**
     * Qué cachés hay que vaciar cuando se importa cada maestro. La calidad depende de los dos: cuenta
     * perfiles y ménsulas (PROFILE) y valores no reconocidos de los dos maestros.
     */
    static final Map<String, List<String>> BY_MASTER = Map.of(
            LovMasterReader.MASTER, List.of(LOV_FAMILIES, QUALITY, QUALITY_BY_EP),
            ProfileMasterReader.MASTER, List.of(EP_OVERVIEW, QUALITY, QUALITY_BY_EP)
    );

    /**
     * Todas, sin repetir.
     */
    static final List<String> ALL = BY_MASTER.values().stream().flatMap(List::stream).distinct().toList();

    private CacheNames() {
    }

    /**
     * Las cachés que dependen de alguno de esos maestros, sin repetir.
     */
    static List<String> forMasters(List<String> masters) {
        return masters.stream()
                .flatMap(master -> BY_MASTER.getOrDefault(master, List.of()).stream())
                .distinct()
                .toList();
    }
}
