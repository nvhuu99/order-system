package com.example.cart.repositories.lock_repo;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface LockRepository {
    Mono<LockResolveType> acquireLock(String owner, String resource, Long expireMs);
}
