package com.example.inventory.main.messaging.reservation_requests;

import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class ReservationRequestsCounter {

    public static List<String> ACTIVE_PROFILES = new ArrayList<>();

    @Value("${HOSTNAME:inventory-service}")
    private String HOSTNAME;

    @Autowired
    private Environment environment;

    @Autowired
    private ReactiveRedisTemplate<String, ProductAvailability> redisTemplate;


    public Mono<Void> increaseHandled(String productId) {
        if (shouldSkip()) {
            return Mono.empty();
        }
        return redisTemplate
            .opsForHash()
            .increment("order-system:reservation_requests_counter:handled:"+HOSTNAME, productId, 1)
            .then()
        ;
    }

    public Mono<Long> getHandled(String productId) {
        return redisTemplate
            .opsForHash()
            .get("order-system:reservation_requests_counter:handled:"+HOSTNAME, productId)
            .switchIfEmpty(Mono.just(0))
            .map(data -> {
                try {
                    return Long.valueOf(String.valueOf(data));
                } catch (Throwable ignored) {
                    return 0L;
                }
            })
        ;
    }


    private Boolean shouldSkip() {
        if (ACTIVE_PROFILES.isEmpty()) {
            ACTIVE_PROFILES = List.of(environment.getActiveProfiles());
        }
        return !ACTIVE_PROFILES.contains("dev") && !ACTIVE_PROFILES.contains("staging");
    }
}
