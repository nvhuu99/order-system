package com.example.cart.repositories.cart_repo.drivers;

import com.example.cart.entities.Cart;
import com.example.cart.repositories.cart_repo.exceptions.SaveCartFailed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class CartRepository implements com.example.cart.repositories.cart_repo.CartRepository {

    @Autowired
    private ReactiveRedisTemplate<String, Cart> redisTemplate;

    private final String keyPrefix = "order-processing-system:carts:";

    @Override
    public Mono<Cart> getCartByUserId(String userId) {
        return redisTemplate.opsForValue().get(keyPrefix + userId);
    }

    @Override
    public Mono<Cart> saveCart(Cart cart) {
        return redisTemplate.opsForValue()
            .set(keyPrefix + cart.getUserId(), cart)
            .flatMap(ok -> ok ? Mono.just(cart) : Mono.error(new SaveCartFailed()));
    }
}
