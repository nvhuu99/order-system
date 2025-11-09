package com.example.inventory.repositories.product_availabilities;

import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        var serializer = new Jackson2JsonRedisSerializer<>(om, ProductAvailability.class);
        var serializationContext = RedisSerializationContext
            .<String, ProductAvailability>newSerializationContext(new StringRedisSerializer())
            .value(serializer)
            .build();
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
