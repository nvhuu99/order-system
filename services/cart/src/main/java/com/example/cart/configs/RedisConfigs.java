package com.example.cart.configs;

import com.example.cart.entities.Cart;
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
    public ReactiveRedisTemplate<String, Cart> cartTemplate(ReactiveRedisConnectionFactory connectionFactory) {

        var serializationContext = RedisSerializationContext
            .<String, Cart>newSerializationContext(new StringRedisSerializer())
            .value(new Jackson2JsonRedisSerializer<>(Cart.class))
            .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
