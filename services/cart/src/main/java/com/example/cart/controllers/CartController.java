package com.example.cart.controllers;

import com.example.cart.services.cart_service.CartUpdateRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("api/v1/carts")
public class CartController {

    @Autowired
    private CartUpdateRequestHandler cartUpdateRequestHandler;

    @GetMapping("cart-update-request-handler/summary")
    public Mono<ResponseEntity<?>> cartUpdateRequestHandlerSummary() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "observed_cart_versions", cartUpdateRequestHandler.getObservedCartVersions(),
            "requests_handled_total", cartUpdateRequestHandler.getRequestsHandledTotal()
        )));
    }
}
