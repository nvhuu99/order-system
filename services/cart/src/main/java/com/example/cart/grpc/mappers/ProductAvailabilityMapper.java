package com.example.cart.grpc.mappers;

import com.example.cart.entities.ProductAvailability;

import java.util.HashMap;
import java.util.Map;

public class ProductAvailabilityMapper {
    public static com.example.grpc.cart.stubs.ProductAvailability mapFromEntity(ProductAvailability entity) {
        return com.example.grpc.cart.stubs.ProductAvailability.newBuilder()
            .setProductId(entity.getProductId())
            .setProductName(entity.getProductName())
            .setPrice(entity.getPrice())
            .setAvailableStock(entity.getAvailableStock())
            .setIsAvailable(entity.getIsAvailable())
            .build();
    }

    public static Map<String, com.example.grpc.cart.stubs.ProductAvailability> mapFromEntities(Map<String, ProductAvailability> entities) {
        var items = new HashMap<String, com.example.grpc.cart.stubs.ProductAvailability>();
        entities.forEach((key, value) -> items.put(key, mapFromEntity(value)));
        return items;
    }
}
