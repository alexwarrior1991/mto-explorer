package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.dto.EpOverviewDto;
import com.alejandro.mtoexplorer.api.dto.ExecutionPackageDto;
import com.alejandro.mtoexplorer.api.dto.StationDto;
import com.alejandro.mtoexplorer.api.dto.TrackDto;
import com.alejandro.mtoexplorer.api.mapper.DtoMapper;
import com.alejandro.mtoexplorer.service.ExecutionPackageService;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/eps")
@Tag(name = "Paquetes", description = "Paquetes de ejecución, sus estaciones y sus vías")
public class ExecutionPackageResource {

    private final ExecutionPackageService service;

    ExecutionPackageResource(ExecutionPackageService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "Todos los paquetes, por código")
    public Uni<List<ExecutionPackageDto>> list() {
        return service.list().map(eps -> DtoMapper.toDtos(eps, DtoMapper::toDto));
    }

    @GET
    @Path("/overview")
    @Operation(summary = "Cada paquete con cuántas vías y perfiles tiene")
    public Uni<List<EpOverviewDto>> overview() {
        return service.overview().map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }

    @GET
    @Path("/{ep}")
    @Operation(summary = "Un paquete")
    public Uni<ExecutionPackageDto> get(@PathParam("ep") String ep) {
        return service.get(ep).map(DtoMapper::toDto);
    }

    @GET
    @Path("/{ep}/stations")
    @Operation(summary = "Las estaciones de un paquete")
    public Uni<List<StationDto>> stations(@PathParam("ep") String ep) {
        return service.stations(ep).map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }

    @GET
    @Path("/{ep}/tracks")
    @Operation(summary = "Las vías de un paquete")
    public Uni<List<TrackDto>> tracks(@PathParam("ep") String ep) {
        return service.tracks(ep).map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }
}
