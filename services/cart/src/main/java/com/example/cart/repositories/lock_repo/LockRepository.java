package com.example.cart.repositories.lock_repo;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface LockRepository {
    Mono<Void> acquireLock(String key, String lock, Duration ttl);
    Mono<Void> releaseLock(String key, String lock);
}
