package com.example.cart.controllers;

import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import com.example.cart.services.cartmanager.internal.CartEventsPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/carts")
public class CartController {

    @Autowired
    private CartEventsPublisher cartEventsPublisher;

    @PostMapping
    public Mono<?> updateCart(@RequestBody CartUpdateRequest request) {
        return cartEventsPublisher.publishCartUpdateRequest(request)
            .map(ok -> ResponseEntity.ok("requested"))
            .onErrorMap(err -> err);
    }
}
