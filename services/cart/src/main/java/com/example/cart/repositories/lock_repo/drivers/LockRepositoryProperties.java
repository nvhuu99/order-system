package com.example.cart.repositories.lock_repo.drivers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class LockRepositoryProperties {

    @Bean(name = "lockAcquireLuaScript")
    public DefaultRedisScript<Long> lockAcquireLuaScript() {
        var script = new DefaultRedisScript<Long>("""
            local key = KEYS[1]
            local lock_value = ARGV[1]
            local ttl = tonumber(ARGV[2])
            local existing_value = redis.call("GET", key)
            if existing_value then
                if existing_value ~= lock_value then
                    return 0
                end
                redis.call("PEXPIRE", key, ttl)
                return 1
            end
            redis.call("SET", key, lock_value, "PX", ttl)
            return 1
        """);
        script.setResultType(Long.class);
        return script;
    }
}