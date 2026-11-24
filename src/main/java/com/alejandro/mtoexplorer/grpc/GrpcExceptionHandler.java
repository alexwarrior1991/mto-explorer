package com.alejandro.mtoexplorer.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.quarkus.grpc.ExceptionHandler;

final class GrpcExceptionHandler<ReqT, RespT> extends ExceptionHandler<ReqT, RespT> {

    GrpcExceptionHandler(ServerCall.Listener<ReqT> listener,
                         ServerCall<ReqT, RespT> call,
                         Metadata metadata) {
        super(listener, call, metadata);
    }

    @Override
    protected void handleException(Throwable failure, ServerCall<ReqT, RespT> call,
                                   Metadata metadata) {
        call.close(GrpcExceptionMapper.toStatus(failure), new Metadata());
    }
}