package com.example.cart.services.cart_service;

import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import org.slf4j.Logger;
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

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
    public void handle(CartUpdateRequest request, Acknowledgment ack, @Headers Map<String, Object> headers) {

        log.info(logTemplate(headers, "Message received"));

        request.setHandlerName(getConsumerName(headers));

        cartUpdateRequestHandler
            .handle(request, () -> ack.acknowledge(), () -> log.info(logTemplate(headers, "Message accepted")))
            .onErrorResume(ex -> Mono.empty())
            .then(gracefullyWait(10, 50))
            .block()
        ;
    }

    private Mono<Void> gracefullyWait(Integer minMs, Integer maxMs) {
        long millis = ThreadLocalRandom.current().nextLong(minMs, maxMs);
        return Mono.delay(Duration.ofMillis(millis)).then();
    }

    private String logTemplate(Map<String, Object> headers, String append) {
        return String.format(
            "key=%s - partition=%s - group=%s - offset=%s - hostname=%s - " + append,
            headers.get(KafkaHeaders.RECEIVED_KEY),
            headers.get(KafkaHeaders.RECEIVED_PARTITION),
            headers.get(KafkaHeaders.GROUP_ID),
            headers.get(KafkaHeaders.OFFSET),
            env.getProperty("HOSTNAME", "unknown")
        );
    }

    private String getConsumerName(Map<String, Object> headers) {
        return String.format("consumer-%s-%s",
            headers.get(KafkaHeaders.GROUP_ID),
            headers.get(KafkaHeaders.RECEIVED_PARTITION)
        );
    }
}
