package com.example.cart.grpc;

import com.example.cart.grpc.mappers.CartMapper;
import com.example.cart.grpc.mappers.CartUpdateRequestMapper;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.services.cart_service.CartEventsPublisher;
import com.example.grpc.cart.stubs.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class CartGrpcService extends CartServiceGrpc.CartServiceImplBase {

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private CartEventsPublisher cartEventsPublisher;

    @Override
    public void getCartByUserId(GetCartByUserIdRequest request, StreamObserver<GetCartByUserIdResult> responseObserver) {

        var result = GetCartByUserIdResult.newBuilder();

        cartRepo.getCartByUserId(request.getUserId())
            .doOnSuccess(cart -> {
                if (cart == null) {
                    responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
                    return;
                }
                responseObserver.onNext(result.setCart(CartMapper.mapFromEntity(cart)).build());
                responseObserver.onCompleted();
            })
            .doOnError(ex ->
                responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException())
            )
            .subscribe();
    }

    @Override
    public void cartUpdateRequest(CartUpdateRequest request, StreamObserver<CartUpdateRequestResult> responseObserver) {

        var result = CartUpdateRequestResult.newBuilder();

        cartEventsPublisher
            .publishCartUpdateRequest(CartUpdateRequestMapper.mapToEntity(request))
            .doOnSuccess(ok -> {
                responseObserver.onNext(result.setSuccess(true).setMessage("Requested").build());
                responseObserver.onCompleted();
            })
            .doOnError(ex ->
                responseObserver.onNext(result.setSuccess(false).setMessage("Failed to request: " + ex.getMessage()).build())
            )
            .subscribe();
    }
}