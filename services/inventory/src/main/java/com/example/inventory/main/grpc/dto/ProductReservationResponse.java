package com.example.inventory.main.grpc.dto;

import com.example.inventory.repositories.product_reservations.entities.ProductReservation;

public class ProductReservationResponse {
    public static com.example.grpc.inventory.stubs.ProductReservation fromEntity(ProductReservation entity) {
        var expiresAt = com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(entity.getExpiresAt().getEpochSecond())
            .setNanos(entity.getExpiresAt().getNano())
            .build();
        var requestedAt = com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(entity.getRequestedAt().getEpochSecond())
            .setNanos(entity.getRequestedAt().getNano())
            .build();
        return com.example.grpc.inventory.stubs.ProductReservation.newBuilder()
            .setId(entity.getId())
            .setProductId(entity.getProductId())
            .setReservedAmount(entity.getReservedAmount())
            .setDesiredAmount(entity.getDesiredAmount())
            .setStatus(entity.getStatus())
            .setExpiresAt(expiresAt)
            .setRequestedAt(requestedAt)
            .build()
        ;
    }
}
