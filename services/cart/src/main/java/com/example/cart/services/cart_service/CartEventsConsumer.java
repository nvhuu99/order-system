package com.example.cart.services.cart_service;

import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class CartEventsConsumer {

    private final Logger log = LoggerFactory.getLogger(CartEventsConsumer.class);

    @Autowired
    private Environment env;

    @Autowired
    private CartUpdateRequestHandler cartUpdateRequestHandler;

    @KafkaListeners({
        @KafkaListener(groupId = "group-0", topicPartitions = {
            @TopicPartition(partitions = { "0" }, topic = "${order-processing-system.messaging.cart-update-requests.topic-name}")
        }),
        @KafkaListener(groupId = "group-1", topicPartitions = {
            @TopicPartition(partitions = { "1" }, topic = "${order-processing-system.messaging.cart-update-requests.topic-name}")
        }),
    })
    public void handle(
        CartUpdateRequest request,
        Acknowledgment ack,
        @Headers Map<String, Object> headers
    ) {
        log.info(logTemplate(headers, "Message received"));

        request.getHandler().setName(env.getProperty("HOSTNAME", "unknown"));

        cartUpdateRequestHandler
            .handle(request, () -> ack.acknowledge(), () -> log.info(logTemplate(headers, "Message accepted")))
            .onErrorResume(ex -> Mono.empty())
            .block()
        ;
    }

    private String logTemplate(Map<String, Object> headers, String append) {
        return String.format(
            "partition=%s - group=%s - offset=%s " + append,
            headers.get(KafkaHeaders.RECEIVED_PARTITION),
            headers.get(KafkaHeaders.GROUP_ID),
            headers.get(KafkaHeaders.OFFSET)
        );
    }
}
