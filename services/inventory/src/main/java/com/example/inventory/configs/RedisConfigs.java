package com.example.inventory.configs;

import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfigs {

    @Bean
    public ReactiveRedisTemplate<String, ProductAvailability> productAvailability(ReactiveRedisConnectionFactory connectionFactory) {
        var serializationContext = RedisSerializationContext
            .<String, ProductAvailability>newSerializationContext(new StringRedisSerializer())
            .value(new Jackson2JsonRedisSerializer<>(ProductAvailability.class))
            .build();
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
