package com.alejandro.mtoexplorer.service.model;

import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.entity.Profile;

import java.util.List;

/** Un perfil con sus ménsulas, por slot. */
public record ProfileWithCantilevers(Profile profile, List<Cantilever> cantilevers) {
}
