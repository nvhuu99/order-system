package com.example.shop.services.cart_service;


import com.example.shop.services.cart_service.entities.CartUpdateRequest;
import com.example.shop.services.cart_service.entities.CartUpdateRequestResult;
import reactor.core.publisher.Mono;

public interface CartService {
    Mono<CartUpdateRequestResult> cartUpdateRequest(CartUpdateRequest request);
}
