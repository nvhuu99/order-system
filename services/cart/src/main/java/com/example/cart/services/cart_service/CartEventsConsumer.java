package com.example.cart.services.cart_service;

import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CartEventsConsumer {

    private final Logger log = LoggerFactory.getLogger(CartEventsConsumer.class);

    @Autowired
    private Environment env;

    @Autowired
    private CartUpdateRequestHandler cartUpdateRequestHandler;

    @KafkaListener(topicPartitions = { @TopicPartition(
        topic = "${order-processing-system.messaging.cart-update-requests.topic-name}",
        partitions = { "0", "1" })
    })
    public void handle(CartUpdateRequest request, Acknowledgment ack, @Headers Map<String, Object> headers) {

        log.info(logTemplate(headers, "Message received"));

        request.setHandlerName(getConsumerName(headers));

        var isCommited = new AtomicBoolean(false);
        var execute = cartUpdateRequestHandler.handle(request, () -> {
            ack.acknowledge();
            isCommited.set(true);
        }, null);

        execute
            .onErrorResume(ex -> Mono.empty())
            .doOnTerminate(() -> {
                if (isCommited.get()) {
                    log.info(logTemplate(headers, "Message committed"));
                } else {
                    log.info(logTemplate(headers, "Did not commit message"));
                }
            })
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
            env.getProperty("HOSTNAME", "cart-service")
        );
    }

    private String getConsumerName(Map<String, Object> headers) {
        return String.format("consumer-%s-%s",
            headers.get(KafkaHeaders.GROUP_ID),
            headers.get(KafkaHeaders.RECEIVED_PARTITION)
        );
    }
}
