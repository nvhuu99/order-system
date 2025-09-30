package com.example.cart.grpc.mappers;

import com.example.cart.entities.properties.CartValidation;
import com.example.grpc.cart.stubs.CartValidationType;

import java.util.HashMap;
import java.util.Map;

public class CartValidationMapper {

    public static com.example.grpc.cart.stubs.CartValidation mapFromEntity(CartValidation entity) {
        if (entity == null) {
            return null;
        }
        return com.example.grpc.cart.stubs.CartValidation.newBuilder()
            .setProductId(entity.getProductId())
            .setType(CartValidationType.valueOf(entity.getType().toString()))
            .setMessage(entity.getMessage())
            .build();
    }

    public static Map<String, com.example.grpc.cart.stubs.CartValidation> mapFromEntities(Map<String, CartValidation> entities) {
        if (entities == null) {
            return Map.of();
        }
        var items = new HashMap<String, com.example.grpc.cart.stubs.CartValidation>();
        entities.forEach((key, value) -> items.put(key, mapFromEntity(value)));
        return items;
    }
}
