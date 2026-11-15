package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.api.dto.LovDto;
import com.alejandro.mtoexplorer.api.dto.LovFamilyDto;
import com.alejandro.mtoexplorer.api.dto.PageDto;
import com.alejandro.mtoexplorer.api.mapper.DtoMapper;
import com.alejandro.mtoexplorer.repository.criteria.LovSearch;
import com.alejandro.mtoexplorer.service.LovService;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

import static com.alejandro.mtoexplorer.api.QueryParams.blankToNull;

@Path("/api/lovs")
@Tag(name = "LOVs", description = "El catálogo de códigos")
public class LovResource {

    private final LovService service;

    LovResource(LovService service) {
        this.service = service;
    }

    @GET
    @Operation(summary = "Códigos del catálogo, filtrables por familia y por texto (código o descripción)")
    public Uni<PageDto<LovDto>> search(@QueryParam("entity") String entity,
                                       @QueryParam("q") String text,
                                       @QueryParam("page") @DefaultValue("0") @Min(0) int page,
                                       @QueryParam("size") @DefaultValue("100") @Min(1) @Max(500) int size) {
        return service.search(new LovSearch(blankToNull(entity), blankToNull(text)), page, size)
                .map(result -> DtoMapper.toDto(result, DtoMapper::toDto));
    }

    @GET
    @Path("/entities")
    @Operation(summary = "Familias del catálogo con cuántos códigos tienen y cuántos quedan por revisar")
    public Uni<List<LovFamilyDto>> families() {
        return service.families().map(list -> DtoMapper.toDtos(list, DtoMapper::toDto));
    }
}
