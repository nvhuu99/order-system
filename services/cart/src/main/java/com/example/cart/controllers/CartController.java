package com.example.cart.controllers;

import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.services.cart_service.CartRequestHandler;
import com.example.cart.services.cart_service.entities.CartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/carts")
public class CartController {

    @Autowired
    private CartRequestHandler cartRequestHandler;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("{userId}")
    public Mono<?> getCart(@PathVariable String userId) {
        return cartRepo.getCartByUserId(userId);
    }

    @PutMapping("{userId}")
    public Mono<?> updateCart(@RequestBody CartRequest request) {
        return cartRequestHandler.handle(request);
    }
}
