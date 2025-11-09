package com.example.shop.services.inventory_client.dto;

import com.example.shop.services.inventory_client.entities.ProductReservation;

import java.time.Instant;

public class ProductReservationResponse {
    public static ProductReservation mapToEntity(com.example.grpc.inventory.stubs.ProductReservation data) {
        return ProductReservation.builder()
            .id(data.getId())
            .userId(data.getUserId())
            .productId(data.getProductId())
            .reservedAmount(data.getReservedAmount())
            .desiredAmount(data.getDesiredAmount())
            .status(data.getStatus())
            .expiredAt(Instant.ofEpochSecond(data.getExpiredAt().getSeconds()))
            .updatedAt(Instant.ofEpochSecond(data.getUpdatedAt().getSeconds()))
            .build()
        ;
    }
}
