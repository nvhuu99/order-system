package com.example.cart.grpc.mappers;

import com.example.cart.entities.Cart;

public class CartMapper {
    public static com.example.grpc.cart.stubs.Cart mapFromEntity(Cart cart) {
        return com.example.grpc.cart.stubs.Cart.newBuilder()
            .setUserId(cart.getUserId())
            .setVersionNumber(cart.getVersionNumber())
            .putAllItems(CartItemMapper.mapFromEntities(cart.getItems()))
            .putAllProductAvailabilities(ProductAvailabilityMapper.mapFromEntities(cart.getProductAvailabilities()))
            .putAllValidations(CartValidationMapper.mapFromEntities(cart.getValidations()))
            .build();
    }
}
