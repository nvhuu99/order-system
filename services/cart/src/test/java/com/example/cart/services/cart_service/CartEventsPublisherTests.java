package com.example.cart.services.cart_service;

import com.example.cart.TestBase;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.cart_service.mocks.CartUpdateRequestKafkaConsumerMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CartEventsPublisherTests extends TestBase {

    @Autowired
    private CartEventsPublisher publisher;

    @Autowired
    private CartUpdateRequestKafkaConsumerMock cartUpdateRequestConsumer;

    private CartUpdateRequest buildRequest() {
        return new CartUpdateRequest(UUID.randomUUID().toString(), 1, List.of(
            new CartUpdateRequest.CartUpdateRequestEntry("PRODUCT_001", "Product_1", 1, CartUpdateRequest.CartAction.QTY_CHANGE),
            new CartUpdateRequest.CartUpdateRequestEntry("PRODUCT_002", "Product_2", 2, CartUpdateRequest.CartAction.DROP_ITEM)
        ), "unknown");
    }

    @Test
    void whenSendingCartUpdateRequest_thenMessageReceived() throws Exception{
        var request = buildRequest();
        publisher.publishCartUpdateRequest(request).block();
        cartUpdateRequestConsumer.getLatch().await(10, TimeUnit.SECONDS);

        assertNotNull(cartUpdateRequestConsumer.getRequest());
        assertEquals(request, cartUpdateRequestConsumer.getRequest());
    }
}
