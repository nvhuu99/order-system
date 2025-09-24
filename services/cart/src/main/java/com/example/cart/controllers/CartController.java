package com.example.cart.controllers;

import com.example.cart.entities.Cart;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.services.cart_service.CartUpdateRequestHandler;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("api/v1/carts")
public class CartController {

    @Autowired
    private CartUpdateRequestHandler cartRequestHandler;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("{userId}")
    public Mono<?> getCart(@PathVariable String userId) {
        return cartRepo.getCartByUserId(userId);
    }

    @PutMapping("{userId}")
    public Mono<ResponseEntity<Cart>> updateCart(@RequestBody CartUpdateRequest request) {
        return cartRequestHandler
            .handle(request)
            .flatMap(cart -> Mono.just(ResponseEntity.ok(cart)))
            .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().body(null)))
            .subscribeOn(Schedulers.boundedElastic());
    }
}
