package com.example.cart.repositories.cart_cache_repo;

import com.example.cart.entities.Cart;
import reactor.core.publisher.Mono;

public interface CartCacheRepository {
    Mono<Cart> getCartByUserId(String userId);
    Mono<Cart> saveCart(Cart cart);
}
