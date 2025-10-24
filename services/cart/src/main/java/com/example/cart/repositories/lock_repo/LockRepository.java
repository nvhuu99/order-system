package com.example.cart.repositories.lock_repo;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface LockRepository {
    Mono<Void> acquireLock(String resource, String lockValue, Duration ttl);
    Mono<Void> releaseLock(String resource, String lockValue);
}
