package com.example.cart.repositories.lock_repo.drivers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class LockRepositoryProperties {

    @Bean(name = "lockAcquireLuaScript")
    public DefaultRedisScript<Long> lockAcquireLuaScript() {
        var script = new DefaultRedisScript<Long>("""
            local keys = KEYS
            local lock_value = ARGV[1]
            local ttl = tonumber(ARGV[2])
            if redis.call("EXISTS", keys[1]) == 1 then
              return 0
            end
            redis.call("SET", keys[1], lock_value, "PX", ttl)
            return 1
        """);
        script.setResultType(Long.class);
        return script;
    }

    @Bean(name = "lockReleaseLuaScript")
    public DefaultRedisScript<Long> lockReleaseLuaScript() {
        var script = new DefaultRedisScript<Long>("""
            local keys = KEYS
            local lock_value = ARGV[1]
            if redis.call("EXISTS", keys[1]) == 0 then
                return 1
            end
            if redis.call("GET", keys[1]) == lock_value then
              redis.call("DEL", keys[1])
              return 1
            end
            return 0
        """);
        script.setResultType(Long.class);
        return script;
    }
}