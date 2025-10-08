package com.example.cart.repositories.lock_repo.drivers;

import com.example.cart.repositories.lock_repo.LockResolveType;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LockRepository implements com.example.cart.repositories.lock_repo.LockRepository {

    private final String keyPrefix = "order-processing-system:locks:";

    private ConcurrentMap<String, String> inMemoryLocks = new ConcurrentHashMap<>();

    @Resource(name = "lockAcquireLuaScript")
    private DefaultRedisScript<Long> acquireScript;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    private static class InMemoryLockMissing extends RuntimeException {}


    @Override
    public Mono<LockResolveType> acquireLock(String owner, String resource, Long expireMs) {

        var key = key(resource);
        var redisKey = redisKey(resource);

        return Mono
            .fromCallable(() -> {
                if (inMemoryLocks.getOrDefault(key, "") != owner) {
                    throw new InMemoryLockMissing();
                }
                return LockResolveType.IN_MEMORY;
            })
            .onErrorResume(InMemoryLockMissing.class, ex ->
                redisTemplate
                    .execute(acquireScript, redisKey, owner, String.valueOf(expireMs))
                    .next()
                    .flatMap(r -> r == 0
                        ? Mono.error(new LockUnavailable(owner, resource))
                        : Mono.just(LockResolveType.CREATE_NEW)
                    )
                    .doOnSuccess(ok -> inMemoryLocks.put(key, owner))
            )
        ;
    }


    private List<String> redisKey(String resource) {
        return Collections.singletonList(key(resource));
    }

    private String key(String resource) {
        var builder = new StringBuilder(keyPrefix.length() + resource.length());
        builder.append(keyPrefix).append(resource);
        return builder.toString();
    }
}