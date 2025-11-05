package com.example.inventory.services.collection_locks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class CollectionLocksServiceImpProperties {

    @Bean(name = "lockLuaScript")
    public DefaultRedisScript<Long> lockLuaScript() {
        var script = new DefaultRedisScript<Long>("""
            local hashKey = ARGV[1]
            local lockValue = ARGV[2]
            local ttlMs = tonumber(ARGV[3] or 0)
            local _unpack = table.unpack or unpack
    
            for i = 1, #KEYS do
                local v = redis.call("HGET", hashKey, KEYS[i])
                if v and v ~= lockValue then
                    return 0
                end
            end
    
            for i = 1, #KEYS do
                redis.call("HSET", hashKey, KEYS[i], lockValue)
            end
    
            if ttlMs > 0 then
                redis.call("HPEXPIRE", hashKey, ttlMs, "FIELDS", #KEYS, _unpack(KEYS))
            end
    
            return 1
        """);
        script.setResultType(Long.class);
        return script;
    }

    @Bean(name = "unlockLuaScript")
    public DefaultRedisScript<Long> unlockLuaScript() {
        var script = new DefaultRedisScript<Long>("""
            local hashKey = ARGV[1]
            local lockValue = ARGV[2]
            local _unpack = table.unpack or unpack

            for i = 1, #KEYS do
                local v = redis.call("HGET", hashKey, KEYS[i])
                if v and v ~= lockValue then
                    return 0
                end
            end

            redis.call("HDEL", hashKey, _unpack(KEYS))
            return 1
        """);
        script.setResultType(Long.class);
        return script;
    }
}