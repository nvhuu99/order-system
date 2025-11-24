package com.example.inventory.main.messaging.reservation_requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReservationRequestsTracker {

    private static final Logger log = LoggerFactory.getLogger(ReservationRequestsTracker.class);
    public static List<String> ACTIVE_PROFILES = new ArrayList<>();

    @Value("${order-system.messaging.product-reservation-requests.track-max-requests}")
    private Integer TRACK_MAX_REQUESTS;

    @Value("${HOSTNAME:inventory-service}")
    private String HOSTNAME;

    @Autowired
    private Environment environment;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;


    public Mono<Void> addToRecentHandledRequests(ReservationRequest request) {
        var key = "order-system:reservation_requests:recent_handled_requests";
        var countMembers = redisTemplate.opsForZSet().size(key);
        var addMembers = redisTemplate.opsForZSet().add(key, request.getRequestId(), request.getRequestedAt().toEpochMilli());
        var popMembers = redisTemplate.opsForZSet().popMin(key);

        return countMembers
            .flatMap(size -> (size >= TRACK_MAX_REQUESTS)
                ? popMembers.then(addMembers)
                : addMembers
            )
            .then()
        ;
    }

    public Mono<Boolean> hasRequestHandledRecently(ReservationRequest request) {
        var key = "order-system:reservation_requests:recent_handled_requests";
        return redisTemplate
            .opsForZSet()
            .score(key, request.getRequestId())
            .switchIfEmpty(Mono.just(0D))
            .map(score -> score > 0D)
        ;
    }


    public Mono<Void> increaseTotalHandledRequests(String productId) {
        if (skipCountingHandledRequests()) {
            return Mono.empty();
        }
        return redisTemplate
            .opsForHash()
            .increment("order-system:reservation_requests:handled_total:"+HOSTNAME, productId, 1)
            .then()
        ;
    }

    public Mono<Long> getTotalHandledRequests(String productId) {
        return redisTemplate
            .opsForHash()
            .get("order-system:reservation_requests:handled_total:"+HOSTNAME, productId)
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


    private Boolean skipCountingHandledRequests() {
        if (ACTIVE_PROFILES.isEmpty()) {
            ACTIVE_PROFILES = List.of(environment.getActiveProfiles());
        }
        return !ACTIVE_PROFILES.contains("dev") && !ACTIVE_PROFILES.contains("staging");
    }
}
