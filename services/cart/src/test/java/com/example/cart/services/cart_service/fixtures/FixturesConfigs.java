package com.example.cart.services.cart_service.fixtures;

import com.example.cart.entities.ProductAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@Configuration
@Profile("test")
public class FixturesConfigs {

    @Bean
    public Map<String, ProductAvailability> productAvailabilities() {
        return Map.of(
            "PRODUCT_001", new ProductAvailability("PRODUCT_001", "", 0.0, 0, false),   // not available
            "PRODUCT_002", new ProductAvailability("PRODUCT_002", "", 0.0, 0, true),    // out of stock
            "PRODUCT_003", new ProductAvailability("PRODUCT_002", "", 0.0, 3, true)     // has some stock
        );
    }
}
