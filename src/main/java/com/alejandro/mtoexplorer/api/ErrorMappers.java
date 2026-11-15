package com.alejandro.mtoexplorer.api;

import com.alejandro.mtoexplorer.load.MasterFormatException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ErrorMappers {

    public record Problem(String title, int status, String detail) {
    }

    @ServerExceptionMapper
    public Response badRequest(IllegalArgumentException e) {
        return problem(400, "Parámetro no válido", e.getMessage());
    }

    @ServerExceptionMapper
    public Response badMaster(MasterFormatException e) {
        return problem(422, "El maestro no tiene la forma esperada", e.getMessage());
    }

    private static Response problem(int status, String title, String detail) {
        return Response.status(status)
                .type(new MediaType("application", "problem+json"))
                .entity(new Problem(title, status, detail))
                .build();
    }
}
