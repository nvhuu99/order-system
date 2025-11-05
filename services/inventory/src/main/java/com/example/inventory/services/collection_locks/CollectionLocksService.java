package com.example.inventory.services.collection_locks;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

public interface CollectionLocksService {

    Mono<Void> tryLock(String collection, List<String> recordIds, String lockValue, Duration ttl);

    Mono<Void> unlock(String collection, List<String> recordIds, String lockValue);

    Mono<Void> forceUnlock(String collection, List<String> recordIds);
}
