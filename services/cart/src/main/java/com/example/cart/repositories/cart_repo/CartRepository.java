package com.example.cart.repositories.cart_repo;

import com.example.cart.entities.Cart;
import reactor.core.publisher.Mono;

public interface CartRepository {
    Mono<Cart> getCartByUserId(String userId);
    Mono<Cart> saveCart(Cart cart);
}
