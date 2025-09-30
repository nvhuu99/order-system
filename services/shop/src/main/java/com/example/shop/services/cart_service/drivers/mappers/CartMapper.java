package com.example.shop.services.cart_service.drivers.mappers;

import com.example.shop.services.cart_service.entities.Cart;

public class CartMapper {

    public static Cart mapToEntity(com.example.grpc.cart.stubs.Cart data) {
        return Cart.builder()
            .userId(data.getUserId())
            .versionNumber(data.getVersionNumber())
            .items(CartItemMapper.mapToEntities(data.getItemsMap()))
            .productAvailabilities(ProductAvailabilityMapper.mapFromEntities(data.getProductAvailabilitiesMap()))
            .validations(CartValidationMapper.mapToEntities(data.getValidationsMap()))
            .build();
    }
}
