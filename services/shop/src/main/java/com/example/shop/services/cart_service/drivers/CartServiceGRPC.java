package com.example.shop.services.cart_service.drivers;

import com.example.grpc.cart.stubs.CartServiceGrpc;
import com.example.shop.services.cart_service.CartService;
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

    @Observed(name = "cart_update_request", lowCardinalityKeyValues = {
        "rpc.system", "grpc",
        "rpc.service", "cart"
    })
    @Override
    public Mono<CartUpdateRequestResult> cartUpdateRequest(CartUpdateRequest request) {

        Sinks.One<CartUpdateRequestResult> sink = Sinks.one();

        var requestBuilder = com.example.grpc.cart.stubs.CartUpdateRequest.newBuilder()
            .setUserId(request.getUserId())
            .setVersionNumber(request.getVersionNumber());

        for (var entry : request.getEntries()) {
            var mappedEntryBuilder = com.example.grpc.cart.stubs.CartUpdateRequest.CartUpdateRequestEntry
                .newBuilder()
                .setAction(com.example.grpc.cart.stubs.CartUpdateRequest.CartAction.valueOf(entry.getAction().toString()))
                .setProductId(entry.getProductId())
                .setProductName(entry.getProductName())
                .setQtyAdjustment(entry.getQtyAdjustment())
            ;
            requestBuilder.addEntries(mappedEntryBuilder.build());
        }

        cartSvcGRPC.cartUpdateRequest(requestBuilder.build(), new StreamObserver<>() {
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
            public void onCompleted() {
            }
        });

        return sink.asMono();
    }
}
