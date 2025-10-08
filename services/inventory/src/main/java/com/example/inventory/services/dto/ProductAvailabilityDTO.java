package com.example.inventory.services.dto;

import com.example.inventory.entities.ProductAvailability;

public class ProductAvailabilityDTO {
    public static com.example.grpc.inventory.stubs.ProductAvailability fromEntity(ProductAvailability entity) {
        return com.example.grpc.inventory.stubs.ProductAvailability.newBuilder()
            .setProductId(entity.getProductId())
            .setProductName(entity.getProductName())
            .setPrice(entity.getPrice())
            .setAvailableStock(entity.getAvailableStock())
            .setIsAvailable(entity.getIsAvailable())
            .build();
    }
}
