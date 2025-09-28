package com.example.cart.services.cart_service;

import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CartEventsConsumer {

    @Autowired
    private CartUpdateRequestHandler cartUpdateRequestHandler;

    @KafkaListener(
        topics = "${order-processing-system.messaging.cart-update-requests.topic-name}",
        groupId = "consumer-group-1"
    )
    public Mono<Void> handle(CartUpdateRequest request, Acknowledgment ack) {
        return cartUpdateRequestHandler.handle(request, ack::acknowledge, null);
    }
}
