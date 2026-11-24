package com.alejandro.mtoexplorer.grpc;

import com.alejandro.mtoexplorer.kp.InvalidKpException;
import com.alejandro.mtoexplorer.kp.Kp;
import com.alejandro.mtoexplorer.service.exception.ResourceNotFoundException;
import io.grpc.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Qué {@link Status} sale de cada excepción. JUnit a secas: no necesita Quarkus. */
class GrpcExceptionMapperTest {

    @Test
    void businessExceptionsHaveTheirOwnCode() {
        assertEquals(Status.Code.NOT_FOUND,
                GrpcExceptionMapper.toStatus(new ResourceNotFoundException("el paquete", "EP9")).getCode());
        InvalidKpException invalidKp = assertThrows(InvalidKpException.class, () -> Kp.parse("12+ABC"));
        assertEquals(Status.Code.INVALID_ARGUMENT, GrpcExceptionMapper.toStatus(invalidKp).getCode());
    }

    @Test
    void aStatusExceptionKeepsItsStatus() {
        Status status = Status.FAILED_PRECONDITION.withDescription("ya importando");
        assertEquals(status, GrpcExceptionMapper.toStatus(status.asRuntimeException()));
        assertEquals(status, GrpcExceptionMapper.toStatus(status.asException()));
    }

    @Test
    void anythingElseIsInternalWithoutDetails() {
        Status status = GrpcExceptionMapper.toStatus(new IllegalStateException("contraseña=1234"));
        assertEquals(Status.Code.INTERNAL, status.getCode());
        assertEquals("Ha ocurrido un error inesperado", status.getDescription());
    }
}
