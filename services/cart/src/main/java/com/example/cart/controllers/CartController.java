package com.example.cart.controllers;

import com.example.cart.entities.Cart;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.services.cart_service.CartEventsPublisher;
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
    private CartEventsPublisher cartEventsPublisher;

    @Autowired
    private CartUpdateRequestHandler cartRequestHandler;

    @Autowired
    private CartRepository cartRepo;

    @GetMapping("{userId}")
    public Mono<?> getCart(@PathVariable String userId) {
        return cartRepo.getCartByUserId(userId);
    }

    @PutMapping("{userId}")
    public Mono<?> updateCart(@RequestBody CartUpdateRequest request) {
        return cartEventsPublisher.publishCartUpdateRequest(request)
            .map(ok -> ResponseEntity.ok("requested"))
            .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError().body(ex.getMessage())))
        ;
    }
}
