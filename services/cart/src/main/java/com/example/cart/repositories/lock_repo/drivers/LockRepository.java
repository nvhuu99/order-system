package com.example.cart.repositories.lock_repo.drivers;

import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.repositories.lock_repo.exceptions.LockValueMismatch;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LockRepository implements com.example.cart.repositories.lock_repo.LockRepository {

    private final String keyPrefix = "order-processing-system:locks:";

    @Resource(name = "lockAcquireLuaScript")
    private DefaultRedisScript<Long> acquireScript;

    @Resource(name = "lockReleaseLuaScript")
    private DefaultRedisScript<Long> releaseScript;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> acquireLock(String resource, String lockValue, Duration ttl) {
        return redisTemplate
            .execute(acquireScript, redisKey(resource), lockValue, String.valueOf(ttl.toMillis()))
            .next()
            .flatMap(r -> r == 0
                ? Mono.error(new LockUnavailable(resource))
                : Mono.empty()
            );
    }

    @Override
    public Mono<Void> releaseLock(String resource, String lockValue) {
        return redisTemplate
            .execute(releaseScript, redisKey(resource), lockValue)
            .next()
            .flatMap(r -> r == 0
                ? Mono.error(new LockValueMismatch(resource))
                : Mono.empty()
            );
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