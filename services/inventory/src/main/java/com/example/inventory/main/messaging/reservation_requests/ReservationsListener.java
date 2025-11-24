package com.example.inventory.main.messaging.reservation_requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ReservationsListener {

    private final Logger log = LoggerFactory.getLogger(ReservationsListener.class);

    @Value("${HOSTNAME:inventory-service}")
    private String hostname;

    @Autowired
    private ReservationsHandler reservationRequestHandler;


    @KafkaListener(topicPartitions = { @TopicPartition(
        topic = "${order-system.messaging.product-reservation-requests.topic-name}",
        partitions = "${order-system.messaging.product-reservation-requests.topic-partitions}")
    })
    public Mono<Void> handle(ReservationRequest request, Acknowledgment ack, @Headers Map<String, Object> headers) {

        log.info(logTemplate(headers, "Message received"));

        var isCommited = new AtomicBoolean(false);
        var execute = reservationRequestHandler.handle(request, (hookName, data) -> {
            if (Objects.equals(hookName, ReservationsHandler.REQUEST_COMMITTED)) {
                ack.acknowledge();
                isCommited.set(true);
            }
        });

        return execute
            .onErrorResume(ex -> Mono.empty())
            .doOnTerminate(() -> {
                if (isCommited.get()) {
                    log.info(logTemplate(headers, "Message committed"));
                } else {
                    log.info(logTemplate(headers, "Did not commit message"));
                }
            })
        ;
    }


    private String logTemplate(Map<String, Object> headers, String append) {
        return String.format(
            "key=%s - partition=%s - group=%s - offset=%s - hostname=%s - " + append,
            headers.get(KafkaHeaders.RECEIVED_KEY),
            headers.get(KafkaHeaders.RECEIVED_PARTITION),
            headers.get(KafkaHeaders.GROUP_ID),
            headers.get(KafkaHeaders.OFFSET),
            hostname
        );
    }
}
