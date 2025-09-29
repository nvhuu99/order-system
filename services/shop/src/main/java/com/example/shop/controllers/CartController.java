package com.example.shop.controllers;

import com.example.shop.services.cart_service.CartService;
import com.example.shop.services.cart_service.entities.CartUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/carts")
public class CartController {

    @Autowired
    private CartService cartSvc;

    @PutMapping("{userId}")
    public Mono<?> updateCart(@RequestBody CartUpdateRequest request) {
        return cartSvc.cartUpdateRequest(request)
            .map(ok -> ResponseEntity.ok("Requested"))
            .onErrorReturn(ResponseEntity.internalServerError().body("Failed to request"))
        ;
    }
}
