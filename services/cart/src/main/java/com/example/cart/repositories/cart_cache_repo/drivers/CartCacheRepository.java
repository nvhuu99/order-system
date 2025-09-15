package com.example.cart.repositories.cart_cache_repo.drivers;

import com.example.cart.entities.Cart;
import com.example.cart.repositories.cart_cache_repo.exceptions.SaveCartFailed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class CartCacheRepository implements com.example.cart.repositories.cart_cache_repo.CartCacheRepository {

    @Autowired
    private ReactiveRedisTemplate<String, Cart> redisTemplate;

    @Value("${order-processing-system.storages.cart-cache.prefix}")
    private String cartCachePrefix;

    @Override
    public Mono<Cart> getCartByUserId(String userId) {
        return redisTemplate.opsForValue().get(cartCachePrefix + userId);
    }

    @Override
    public Mono<Cart> saveCart(Cart cart) {
        return redisTemplate.opsForValue()
            .set(cartCachePrefix + cart.getUserId(), cart)
            .flatMap(ok -> ok ? Mono.just(cart) : Mono.error(new SaveCartFailed()));
    }
}
