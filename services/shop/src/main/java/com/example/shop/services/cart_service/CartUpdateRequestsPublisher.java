package com.example.shop.services.cart_service;

import com.example.shop.services.cart_service.dto.CartUpdateRequest;
import com.example.shop.services.cart_service.dto.ReservationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
public class CartUpdateRequestsPublisher {

    @Value("${order-processing-system.messaging.product-reservation-requests.topic-name}")
    private String TOPIC_NAME;

    @Value("${order-processing-system.messaging.product-reservation-requests.timeout-seconds}")
    private Integer TIMEOUT_SECONDS;

    @Autowired
    private KafkaTemplate<String, ReservationRequest> kafka;

    public Mono<Void> publishRequest(CartUpdateRequest request) {
        var now = Instant.now();
        return Flux
            .fromIterable(request.getEntries())
            .flatMap(entry -> {
                var reservationRequest = new ReservationRequest();
                reservationRequest.setUserId(request.getUserId());
                reservationRequest.setProductId(entry.getProductId());
                reservationRequest.setQuantity(entry.getQuantity());
                reservationRequest.setRequestedAt(now);
                return Mono.fromFuture(kafka.send(TOPIC_NAME, request.getUserId(), reservationRequest));
            })
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .then()
        ;
    }
}
