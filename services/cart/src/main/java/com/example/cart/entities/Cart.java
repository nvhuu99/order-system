package com.example.cart.entities;

import com.example.cart.entities.properties.CartValidation;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Data
    @AllArgsConstructor
    public static final class CartItem {
        private String productId;
        private String productName;
        private Integer quantity;
    }

    String userId;
    Integer versionNumber;
    Map<String, CartItem> items;
    Map<String, ProductAvailability> productAvailabilities;
    Map<String, CartValidation> validations;

    public Cart(String userId) {
        this.userId = userId;
        this.versionNumber = 0;
        items = new HashMap<>();
        productAvailabilities = new HashMap<>();
        validations = new HashMap<>();
    }

    @JsonIgnore
    public ProductAvailability getProductAvailability(String productId) {
        return productAvailabilities.get(productId);
    }
}
