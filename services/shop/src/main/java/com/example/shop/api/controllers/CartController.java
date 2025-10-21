package com.example.shop.api.controllers;

import com.example.shop.api.responses.ApiResponse;
import com.example.shop.services.cart_service.CartService;
import com.example.shop.services.cart_service.entities.CartUpdateRequest;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/carts")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    @Autowired
    private CartService cartSvc;

    @GetMapping("{userId}")
    public Mono<ResponseEntity<ApiResponse>> getCart(@PathVariable String userId) {
        return cartSvc.getCartByUserId(userId)
            .map(ApiResponse::ok)
            .doOnError(ex -> log.error(ex.getMessage()))
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
            .doOnError(ex -> log.error(ex.getMessage()))
            .onErrorResume(ex -> Mono.just(ApiResponse.internalServerError(ex)))
        ;
    }
}
