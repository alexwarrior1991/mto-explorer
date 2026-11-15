package com.alejandro.mtoexplorer.service;

import com.alejandro.mtoexplorer.config.ExplorerConfig;
import com.alejandro.mtoexplorer.entity.Profile;
import com.alejandro.mtoexplorer.repository.ProfileRepository;
import com.alejandro.mtoexplorer.service.exception.ResourceNotFoundException;
import com.alejandro.mtoexplorer.service.model.Page;
import com.alejandro.mtoexplorer.repository.criteria.ProfileSearch;
import com.alejandro.mtoexplorer.service.model.ProfileDetail;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Búsqueda de perfiles y ficha de un perfil con sus ménsulas. */
@ApplicationScoped
public class ProfileService {

    private final ProfileRepository profiles;
    /** Tamaño de cada página que pide el flujo de {@link #stream} (mto.stream-page-size). */
    private final int streamPageSize;

    ProfileService(ProfileRepository profiles, ExplorerConfig config) {
        this.profiles = profiles;
        this.streamPageSize = config.streamPageSize();
    }

    @WithSession
    public Uni<Page<Profile>> search(ProfileSearch search, int page, int size) {
        // Una sesión reactiva no admite dos consultas a la vez: primero la página, después el total.
        return profiles.search(search).page(page, size).list()
                .chain(items -> profiles.count(search).map(total -> new Page<>(items, page, size, total)));
    }

    /**
     * El perfil con sus ménsulas y sus seccionadores. Las dos colecciones son LAZY, y en Hibernate
     * Reactive no se cargan solas al tocarlas (fallaría con LazyInitializationException):
     * {@code Mutiny.fetch} las pide a la base de datos, una detrás de otra, y devuelve un {@code Uni}
     * con cada lista ya cargada y ordenada por su {@code @OrderBy}.
     */
    @WithSession
    public Uni<ProfileDetail> get(String ep, String track, int order) {
        return profiles.findByTrackAndOrder(ep, track, order)
                .onItem().ifNull().failWith(() ->
                        new ResourceNotFoundException("el perfil", ep + " / " + track + " / " + order))
                .chain(profile -> Mutiny.fetch(profile.cantilevers)
                        .chain(cantilevers -> Mutiny.fetch(profile.disconnectors)
                                .map(disconnectors -> new ProfileDetail(profile, cantilevers, disconnectors))));
    }

    /**
     * Todos los perfiles que cumplen la búsqueda, como un flujo ({@link Multi}) en vez de una lista.
     * Se piden a la base de datos por páginas (mto.stream-page-size, 500 por defecto), y cada perfil
     * sale en cuanto llega su página: quien consume no espera a tenerlos todos.
     *
     * <ul>
     *   <li>{@code repeating().uni(...)} pide una página cada vez que el consumidor quiere más.
     *   <li>{@code until(List::isEmpty)} termina el flujo con la primera página vacía.
     *   <li>{@code disjoint()} convierte cada página (una lista) en sus perfiles, uno a uno.
     * </ul>
     *
     * <p>Cada página va en su propia sesión ({@code Panache.withSession}): {@code @WithSession}
     * solo vale para métodos que devuelven {@code Uni}.
     */
    public Multi<Profile> stream(ProfileSearch search) {
        AtomicInteger page = new AtomicInteger();
        return Multi.createBy().repeating()
                .uni(() -> Panache.withSession(() ->
                        profiles.search(search).page(page.getAndIncrement(), streamPageSize).list()))
                .until(List::isEmpty)
                .onItem().<Profile>disjoint();
    }
}
