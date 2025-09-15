package com.example.cart.services.cartmanager.drivers;

import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class CartEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(CartEventsConsumer.class);

    @Autowired
    private CartUpdateRequestHandler cartUpdateRequestHandler;

    @KafkaListener(
        topics = "${order-processing-system.messaging.cart-update-requests.topic-name}",
        groupId = "${order-processing-system.messaging.cart-update-requests.consumer-group-name}"
    )
    public void cartUpdateRequests(CartUpdateRequest request, Acknowledgment ack) {
        log.info("Handling cart update request");
        cartUpdateRequestHandler.handle(request)
            .doOnError(ex ->
                log.error("Cart update request failed - UserID: {} - Message: {}", request.getUserId(), ex.getMessage())
            )
            .doOnSuccess(ok -> {
                log.info("Cart update request success - UserID: {}", request.getUserId());
                ack.acknowledge();
            })
            .doOn
            .onErrorResume(ex -> Mono .delay(Duration.ofSeconds(1)).then(Mono.error(ex))) // grace wait
            .block();
    }
}
