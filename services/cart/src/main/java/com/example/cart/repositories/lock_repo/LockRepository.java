package com.example.cart.repositories.lock_repo;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public interface LockRepository {
    Mono<String> acquireLock(String key, Duration ttl);
    Mono<Void> releaseLock(String key, AtomicReference<String> lock);
}
