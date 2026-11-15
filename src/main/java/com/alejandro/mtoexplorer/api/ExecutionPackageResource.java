package com.alejandro.mtoexplorer.api;


import com.alejandro.mtoexplorer.api.Dtos.ExecutionPackageDto;
import com.alejandro.mtoexplorer.api.Dtos.StationDto;
import com.alejandro.mtoexplorer.api.Dtos.TrackDto;
import com.alejandro.mtoexplorer.entity.ExecutionPackage;
import com.alejandro.mtoexplorer.entity.Station;
import com.alejandro.mtoexplorer.entity.Track;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import java.util.List;

@Path("api/eps")
@WithSession
public class ExecutionPackageResource {

    @GET
    public Uni<List<ExecutionPackageDto>> list(){
        return ExecutionPackage.listAllByCode()
                .map(eps -> eps.stream().map(ExecutionPackageDto::of).toList());
    }

    @GET
    @Path("/{ep}")
    public Uni<ExecutionPackageDto> get(@PathParam("ep") String ep) {
        return existing(ep).map(ExecutionPackageDto::of);
    }

    @GET
    @Path("/{ep}/stations")
    public Uni<List<StationDto>> stations(@PathParam("ep") String ep){
        return existing(ep)
                .chain(() -> Station.listByEp(ep))
                .map(stations -> stations.stream().map(StationDto::of).toList());
    }

    @GET
    @Path("/{ep}/tracks")
    public Uni<List<TrackDto>> tracks(@PathParam("ep") String ep) {
        return existing(ep)
                .chain(() -> Track.listByEp(ep))
                .map(tracks -> tracks.stream().map(TrackDto::of).toList());
    }

    /** El paquete, o un 404 si no existe. */
    private static Uni<ExecutionPackage> existing(String ep) {
        return ExecutionPackage.findByCode(ep)
                .onItem().ifNull().failWith(() -> new NotFoundException("No existe el paquete " + ep));
    }
}
