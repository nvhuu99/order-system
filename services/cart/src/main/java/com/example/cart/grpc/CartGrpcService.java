package com.example.cart.grpc;

import com.example.cart.grpc.mappers.CartMapper;
import com.example.cart.grpc.mappers.CartUpdateRequestMapper;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.services.cart_service.CartEventsPublisher;
import com.example.grpc.cart.stubs.*;
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
        cartRepo.getCartByUserId(request.getUserId())
            .doOnSuccess(cart -> {
                responseObserver.onNext(
                    GetCartByUserIdResult.newBuilder()
                        .setCart(CartMapper.mapFromEntity(cart))
                        .build()
                );
                responseObserver.onCompleted();
            })
            .doOnError(ex -> {
                responseObserver.onNext(null);
                responseObserver.onCompleted();
            })
            .subscribe();
    }

    @Override
    public void cartUpdateRequest(CartUpdateRequest request, StreamObserver<CartUpdateRequestResult> responseObserver) {
        cartEventsPublisher
            .publishCartUpdateRequest(CartUpdateRequestMapper.mapToEntity(request))
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