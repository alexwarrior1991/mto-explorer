package com.alejandro.mtoexplorer.service.model;

import com.alejandro.mtoexplorer.entity.Cantilever;
import com.alejandro.mtoexplorer.entity.Disconnector;
import com.alejandro.mtoexplorer.entity.Profile;

import java.util.List;

/** Un perfil con sus ménsulas (por slot) y los seccionadores que cuelgan de él (por nombre). */
public record ProfileDetail(Profile profile, List<Cantilever> cantilevers, List<Disconnector> disconnectors) {
}
