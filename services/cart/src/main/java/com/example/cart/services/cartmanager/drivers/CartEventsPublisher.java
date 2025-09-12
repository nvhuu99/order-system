package com.example.cart.services.cartmanager.drivers;

import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CartEventsPublisher implements com.example.cart.services.cartmanager.internal.CartEventsPublisher {

    @Value("${order-processing-system.messaging.cart-update-requests.topic-name}")
    private String cartUpdateRequestsTopicName;

    @Autowired
    private KafkaTemplate<String, CartUpdateRequest> kafkaTemplate;

    @Override
    public Mono<Void> publishCartUpdateRequest(CartUpdateRequest request) {
        return Mono.fromFuture(
            () -> kafkaTemplate.send(cartUpdateRequestsTopicName, request)
        ).then();
    }
}
