package com.example.cart.grpc.mappers;

import com.example.cart.entities.Cart.*;

import java.util.HashMap;
import java.util.Map;

public class CartItemMapper {
    public static com.example.grpc.cart.stubs.CartItem mapFromEntity(CartItem entity) {
        return com.example.grpc.cart.stubs.CartItem.newBuilder()
            .setProductId(entity.getProductId())
            .setProductName(entity.getProductName())
            .setQuantity(entity.getQuantity())
            .build();
    }

    public static Map<String, com.example.grpc.cart.stubs.CartItem> mapFromEntities(Map<String, CartItem> entities) {
        var items = new HashMap<String, com.example.grpc.cart.stubs.CartItem>();
        entities.forEach((key, value) -> items.put(key, mapFromEntity(value)));
        return items;
    }
}
