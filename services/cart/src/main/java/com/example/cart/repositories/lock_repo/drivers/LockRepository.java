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

@Component
public class LockRepository implements com.example.cart.repositories.lock_repo.LockRepository {

    @Resource(name = "lockKeyPrefix")
    private String lockKeyPrefix;

    @Resource(name = "lockAcquireLuaScript")
    private DefaultRedisScript<Long> acquireScript;

    @Resource(name = "lockReleaseLuaScript")
    private DefaultRedisScript<Long> releaseScript;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    private List<String> key(String resource) {
        return Collections.singletonList(lockKeyPrefix + resource);
    }

    @Override
    public Mono<Void> acquireLock(String target, String lockValue, Duration ttl) {
        return redisTemplate
            .execute(acquireScript, key(target), lockValue, String.valueOf(ttl.toMillis()))
            .next()
            .flatMap(r -> r == 0
                ? Mono.error(new LockUnavailable(target))
                : Mono.empty()
            );
    }

    @Override
    public Mono<Void> releaseLock(String target, String lockValue) {
        return redisTemplate
            .execute(releaseScript, key(target), lockValue)
            .next()
            .flatMap(r -> r == 0
                ? Mono.error(new LockValueMismatch(target))
                : Mono.empty()
            );
    }
}
