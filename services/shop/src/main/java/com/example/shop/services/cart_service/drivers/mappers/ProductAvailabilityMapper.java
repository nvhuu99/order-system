package com.example.shop.services.cart_service.drivers.mappers;

import com.example.shop.services.cart_service.entities.ProductAvailability;

import java.util.HashMap;
import java.util.Map;

public class ProductAvailabilityMapper {
    public static ProductAvailability mapToEntity(com.example.grpc.cart.stubs.ProductAvailability data) {
        if (data == null) {
            return null;
        }
        return ProductAvailability.builder()
            .productId(data.getProductId())
            .productName(data.getProductName())
            .price(data.getPrice())
            .availableStock(data.getAvailableStock())
            .isAvailable(data.getIsAvailable())
            .build();
    }

    public static Map<String, ProductAvailability> mapFromEntities(Map<String, com.example.grpc.cart.stubs.ProductAvailability> data) {
        if (data == null) {
            return Map.of();
        }
        var items = new HashMap<String, ProductAvailability>();
        data.forEach((key, value) -> items.put(key, mapToEntity(value)));
        return items;
    }
}
