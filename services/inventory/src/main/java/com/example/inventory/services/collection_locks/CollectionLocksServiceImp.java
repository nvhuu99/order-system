package com.example.inventory.services.collection_locks;

import com.example.inventory.services.collection_locks.exceptions.LockValueMismatch;
import com.example.inventory.services.collection_locks.exceptions.LocksUnavailable;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
public class CollectionLocksServiceImp implements CollectionLocksService {

    private final String keyPrefix = "order-system:locks:";

    @Resource(name = "lockLuaScript")
    private DefaultRedisScript<Long> lockLuaScript;

    @Resource(name = "unlockLuaScript")
    private DefaultRedisScript<Long> unlockLuaScript;

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;


    @Override
    public Mono<Void> tryLock(String collection, List<String> recordIds, String lockValue, Duration ttl) {
        return redisTemplate
            .execute(lockLuaScript, recordIds, redisKey(collection), lockValue, String.valueOf(ttl.toMillis()))
            .next()
            .flatMap(r -> r == 0
                ? Mono.error(new LocksUnavailable())
                : Mono.empty()
            )
        ;
    }

    @Override
    public Mono<Void> unlock(String collection, List<String> recordIds, String lockValue) {
        return redisTemplate
            .execute(unlockLuaScript, recordIds, redisKey(collection), lockValue)
            .next()
            .flatMap(r -> r == 0
                ? Mono.error(new LockValueMismatch())
                : Mono.empty()
            )
        ;
    }

    @Override
    public Mono<Void> forceUnlock(String collection, List<String> recordIds) {
        return redisTemplate
            .opsForHash().remove(redisKey(collection), recordIds.toArray())
            .then()
        ;
    }


    private String redisKey(String collectionName) {
        return keyPrefix + collectionName;
    }
}