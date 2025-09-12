package com.example.cart.repositories;

import com.example.cart.entities.Cart;
import reactor.core.publisher.Mono;

public interface CartPermanentRepository {
    Mono<Cart> getCartById(String id);
}
