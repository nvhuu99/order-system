package com.example.shop.api.controllers;

import com.example.shop.api.responses.ApiResponse;
import com.example.shop.services.cart_service.CartService;
import com.example.shop.services.cart_service.entities.CartUpdateRequest;
import io.grpc.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/carts")
public class CartController {

    @Autowired
    private CartService cartSvc;

    @GetMapping("{userId}")
    public Mono<ResponseEntity<ApiResponse>> getCart(@PathVariable String userId) {
        return cartSvc.getCartByUserId(userId)
            .map(cart -> ApiResponse.ok(cart))
            .onErrorResume(ex -> {
                if (ex instanceof io.grpc.StatusRuntimeException grpcEx) {
                    if (grpcEx.getStatus() == Status.NOT_FOUND) {
                        return Mono.just(ApiResponse.notFound());
                    }
                }
                return Mono.just(ApiResponse.internalServerError(ex));
            });
    }

    @PutMapping("{userId}")
    public Mono<?> updateCart(@RequestBody CartUpdateRequest request) {
        return cartSvc.cartUpdateRequest(request)
            .map(ok -> ApiResponse.ok(null))
            .onErrorResume(ex -> Mono.just(ApiResponse.internalServerError(ex)))
        ;
    }
}
