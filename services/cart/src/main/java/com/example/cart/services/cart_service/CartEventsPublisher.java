package com.example.cart.services.cart_service;

import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CartEventsPublisher {

    @Value("${order-processing-system.messaging.cart-update-requests.topic-name}")
    private String cartUpdateRequestsTopicName;

    @Autowired
    private KafkaTemplate<String, CartUpdateRequest> kafkaTemplate;

    public Mono<Void> publishCartUpdateRequest(CartUpdateRequest request) {
        return Mono.fromFuture(() -> kafkaTemplate.send(cartUpdateRequestsTopicName, request)).then();
    }
}