package com.example.cart.controllers;

import com.example.cart.repositories.cart_cache_repo.CartCacheRepository;
import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import com.example.cart.services.cartmanager.internal.CartEventsPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/carts")
public class CartController {

    @Autowired
    private CartEventsPublisher cartEventsPublisher;

    @Autowired
    private CartCacheRepository cartCache;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("{userId}")
    public Mono<?> getCart(@PathVariable String userId) {
        return cartCache.getCartByUserId(userId);
    }

    @PutMapping("{userId}")
    public Mono<?> updateCart(@RequestBody CartUpdateRequest request) {
        return cartEventsPublisher.publishCartUpdateRequest(request)
            .map(ok -> ResponseEntity.ok("requested"))
            .onErrorMap(err -> err);
    }
}
