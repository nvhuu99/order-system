package com.example.shop.services.cart_service.drivers;

import com.example.grpc.cart.stubs.CartServiceGrpc;
import com.example.grpc.cart.stubs.GetCartByUserIdRequest;
import com.example.shop.services.cart_service.CartService;
import com.example.shop.services.cart_service.drivers.mappers.CartMapper;
import com.example.shop.services.cart_service.drivers.mappers.CartUpdateRequestMapper;
import com.example.shop.services.cart_service.entities.Cart;
import com.example.shop.services.cart_service.entities.CartUpdateRequest;
import com.example.shop.services.cart_service.entities.CartUpdateRequestResult;
import io.grpc.stub.StreamObserver;
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class CartServiceGRPC implements CartService {

    @Autowired
    private CartServiceGrpc.CartServiceStub cartSvcGRPC;

    @Observed(name = "get_cart_by_user_id", lowCardinalityKeyValues = {
        "rpc.system", "grpc",
        "rpc.service", "cart"
    })
    @Override
    public Mono<Cart> getCartByUserId(String userId) {

        Sinks.One<Cart> sink = Sinks.one();

        var request = GetCartByUserIdRequest.newBuilder().setUserId(userId).build();
        cartSvcGRPC.getCartByUserId(request, new StreamObserver<>() {
            @Override
            public void onNext(com.example.grpc.cart.stubs.GetCartByUserIdResult data) {
                sink.tryEmitValue(CartMapper.mapToEntity(data.getCart()));
            }

            @Override
            public void onError(Throwable t) { sink.tryEmitError(t); }

            @Override
            public void onCompleted() {}
        });

        return sink.asMono();
    }

    @Observed(name = "cart_update_request", lowCardinalityKeyValues = {
        "rpc.system", "grpc",
        "rpc.service", "cart"
    })
    @Override
    public Mono<CartUpdateRequestResult> cartUpdateRequest(CartUpdateRequest request) {

        Sinks.One<CartUpdateRequestResult> sink = Sinks.one();

        cartSvcGRPC.cartUpdateRequest(CartUpdateRequestMapper.mapFromEntity(request), new StreamObserver<>() {
            @Override
            public void onNext(com.example.grpc.cart.stubs.CartUpdateRequestResult data) {
                if (! data.getSuccess()) {
                    sink.tryEmitError(new RuntimeException(data.getMessage()));
                } else {
                    sink.tryEmitValue(new CartUpdateRequestResult(true, data.getMessage()));
                }
            }

            @Override
            public void onError(Throwable t) {
                sink.tryEmitError(t);
            }

            @Override
            public void onCompleted() {}
        });

        return sink.asMono();
    }
}
