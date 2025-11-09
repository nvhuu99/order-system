package com.example.inventory.main.grpc.dto;

import com.example.inventory.repositories.product_reservations.entities.ProductReservation;

public class ProductReservationResponse {
    public static com.example.grpc.inventory.stubs.ProductReservation fromEntity(ProductReservation entity) {
        var expiredAt = com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(entity.getExpiredAt().getEpochSecond())
            .setNanos(entity.getExpiredAt().getNano())
            .build();
        var updatedAt = com.google.protobuf.Timestamp.newBuilder()
            .setSeconds(entity.getUpdatedAt().getEpochSecond())
            .setNanos(entity.getUpdatedAt().getNano())
            .build();
        return com.example.grpc.inventory.stubs.ProductReservation.newBuilder()
            .setId(entity.getId())
            .setProductId(entity.getProductId())
            .setReserved(entity.getReserved())
            .setDesiredAmount(entity.getDesiredAmount())
            .setStatus(entity.getStatus())
            .setExpiredAt(expiredAt)
            .setUpdatedAt(updatedAt)
            .build()
        ;
    }
}
