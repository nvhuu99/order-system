package com.example.cart.services.cart_service.mocks;

import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import lombok.Getter;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
@Getter
public class CartUpdateRequestKafkaConsumerMock {

    private CartUpdateRequest request;
    private CountDownLatch latch = new CountDownLatch(1);

    @KafkaListener(topics = "${order-processing-system.messaging.cart-update-requests.topic-name}")
    public void handle(CartUpdateRequest request, Acknowledgment ack) {
        this.request = request;
        ack.acknowledge();
        latch.countDown();
    }
}
