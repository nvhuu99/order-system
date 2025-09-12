package com.example.cart.services.cartmanager.internal;

import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import reactor.core.publisher.Mono;

public interface CartEventsPublisher {
    Mono<Void> publishCartUpdateRequest(CartUpdateRequest request);
}
