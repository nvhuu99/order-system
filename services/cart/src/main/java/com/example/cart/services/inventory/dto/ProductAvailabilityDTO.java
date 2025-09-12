package com.example.cart.services.inventory.dto;

import com.example.cart.entities.ProductAvailability;

public class ProductAvailabilityDTO {
    public static ProductAvailability map(com.example.grpc.inventory.stubs.ProductAvailability data) {
        return new ProductAvailability(
            data.getProductId(),
            data.getProductName(),
            data.getPrice(),
            data.getAvailableStock(),
            data.getIsAvailable()
        );
    }
}
