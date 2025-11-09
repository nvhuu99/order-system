package com.example.shop.services.cart_service;


import com.example.shop.services.cart_service.entities.Cart;
import com.example.shop.services.cart_service.dto.CartUpdateRequest;
import reactor.core.publisher.Mono;

public interface CartService {
    Mono<Cart> getCartByUserId(String userId);
    Mono<Void> cartUpdateRequest(CartUpdateRequest request);
}
