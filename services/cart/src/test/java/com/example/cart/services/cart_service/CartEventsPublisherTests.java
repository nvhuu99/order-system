package com.example.cart.services.cart_service;

import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.cart_service.mocks.CartUpdateRequestKafkaConsumerMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
public class CartEventsPublisherTests {

    @Autowired
    private CartEventsPublisher publisher;

    @Autowired
    private CartUpdateRequestKafkaConsumerMock cartUpdateRequestConsumer;

    private CartUpdateRequest buildRequest() {
        return new CartUpdateRequest("USR_01", 1, List.of(
            new CartUpdateRequest.CartUpdateRequestEntry("PRODUCT_001", "Product_1", 1, CartUpdateRequest.CartAction.QTY_CHANGE),
            new CartUpdateRequest.CartUpdateRequestEntry("PRODUCT_002", "Product_2", 2, CartUpdateRequest.CartAction.DROP_ITEM)
        ));
    }

    @Test
    void whenSendingCartUpdateRequest_thenMessageReceived() throws Exception{
        var request = buildRequest();
        publisher.publishCartUpdateRequest(buildRequest()).block();
        cartUpdateRequestConsumer.getLatch().await(10, TimeUnit.SECONDS);

        assertNotNull(cartUpdateRequestConsumer.getRequest());
        assertEquals(request, cartUpdateRequestConsumer.getRequest());
    }
}
