package com.example.cart.entities;

import com.example.cart.entities.properties.CartValidation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.Map;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cart {

    @Data
    @AllArgsConstructor
    public static final class CartItem {
        private String productId;
        private String productName;
        private Integer quantity;
    }

    public record CartUserInfo(String userId) {}

    String cartId;
    Integer versionNumber;
    CartUserInfo userInfo;
    Map<String, CartItem> items;
    Map<String, ProductAvailability> productAvailabilities;
    Map<String, CartValidation> validations;

    public Cart(String cartId, String userId) {
        this.cartId = cartId;
        userInfo = new CartUserInfo(userId);
        productAvailabilities = new HashMap<>();
        validations = new HashMap<>();
    }

    public ProductAvailability getProductAvailability(String productId) {
        return productAvailabilities.get(productId);
    }
}
