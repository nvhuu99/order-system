package com.example.shop.services.cart_service.drivers.mappers;

import com.example.shop.services.cart_service.entities.Cart.*;

import java.util.HashMap;
import java.util.Map;

public class CartItemMapper {
    public static CartItem mapToEntity(com.example.grpc.cart.stubs.CartItem data) {
        if (data == null) {
            return null;
        }
        return CartItem.builder()
            .productId(data.getProductId())
            .productName(data.getProductName())
            .quantity(data.getQuantity())
            .build();
    }

    public static Map<String, CartItem> mapToEntities(Map<String, com.example.grpc.cart.stubs.CartItem> data) {
        if (data == null) {
            return Map.of();
        }
        var items = new HashMap<String, CartItem>();
        data.forEach((key, value) -> items.put(key, mapToEntity(value)));
        return items;
    }
}
