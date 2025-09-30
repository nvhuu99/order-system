package com.example.shop.services.cart_service.drivers.mappers;

import com.example.shop.services.cart_service.entities.properties.CartValidation;
import com.example.shop.services.cart_service.entities.properties.CartValidation.CartValidationType;

import java.util.HashMap;
import java.util.Map;

public class CartValidationMapper {

    public static CartValidation mapToEntity(com.example.grpc.cart.stubs.CartValidation data) {
        return CartValidation.builder()
            .productId(data.getProductId())
            .type(CartValidationType.valueOf(data.getType().toString()))
            .message(data.getMessage())
            .build();
    }

    public static Map<String, CartValidation> mapToEntities(Map<String, com.example.grpc.cart.stubs.CartValidation> data) {
        var items = new HashMap<String, CartValidation>();
        data.forEach((key, value) -> items.put(key, mapToEntity(value)));
        return items;
    }
}
