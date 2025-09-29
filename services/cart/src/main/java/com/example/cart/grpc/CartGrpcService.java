package com.example.cart.grpc;

import com.example.cart.grpc.dto.CartUpdateRequestDTO;
import com.example.cart.services.cart_service.CartEventsPublisher;
import com.example.grpc.cart.stubs.CartServiceGrpc;
import com.example.grpc.cart.stubs.CartUpdateRequest;
import com.example.grpc.cart.stubs.CartUpdateRequestResult;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class CartGrpcService extends CartServiceGrpc.CartServiceImplBase {

    @Autowired
    private CartEventsPublisher cartEventsPublisher;

    @Override
    public void cartUpdateRequest(
        CartUpdateRequest request,
        StreamObserver<CartUpdateRequestResult> responseObserver
    ) {
        cartEventsPublisher
            .publishCartUpdateRequest(CartUpdateRequestDTO.mapToEntity(request))
            .doOnSuccess(ok -> {
                responseObserver.onNext(
                    CartUpdateRequestResult.newBuilder()
                        .setSuccess(true)
                        .setMessage("Requested")
                        .build()
                );
                responseObserver.onCompleted();
            })
            .doOnError(ex -> {
                responseObserver.onNext(
                    CartUpdateRequestResult.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to request: " + ex.getMessage())
                        .build()
                );
                responseObserver.onCompleted();
            })
            .subscribe();
    }
}