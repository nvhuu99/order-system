package com.example.cart.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfigs {

    @Value("${order-processing-system.messaging.cart-update-requests.topic-name}")
    private String cartUpdateRequestTopic;

    @Bean
    public NewTopic cartUpdateRequestsTopic() {
        return new NewTopic(cartUpdateRequestTopic, 2, (short) 1);
    }
}
