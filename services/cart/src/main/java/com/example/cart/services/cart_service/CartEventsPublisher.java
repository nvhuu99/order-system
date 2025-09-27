package com.example.cart.services.cart_service;

import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Component
public class CartEventsPublisher {

//    @Value("${order-processing-system.messaging.cart-update-requests.topic-name}")
    private final String cartUpdateRequestsTopicName = "cart-request";

    @Autowired
    private KafkaTemplate<String, CartUpdateRequest> kafkaTemplate;

    public Mono<Void> publishCartUpdateRequest(CartUpdateRequest request) {
        return Mono.fromFuture(
            () -> kafkaTemplate.send(cartUpdateRequestsTopicName, request)
        ).then();
    }
}