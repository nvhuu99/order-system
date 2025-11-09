package com.example.shop.services.inventory.dto;

import com.example.shop.services.inventory.entities.ProductReservation;

import java.time.Instant;

public class ProductReservationResponse {
    public static ProductReservation mapToEntity(com.example.grpc.inventory.stubs.ProductReservation data) {
        var entity = new ProductReservation();
        entity.setId(data.getId());
        entity.setUserId(data.getUserId());
        entity.setProductId(data.getProductId());
        entity.setReserved(data.getReserved());
        entity.setDesiredAmount(data.getDesiredAmount());
        entity.setStatus(data.getStatus());
        entity.setExpiredAt(Instant.ofEpochSecond(data.getExpiredAt().getSeconds()));
        entity.setUpdatedAt(Instant.ofEpochSecond(data.getUpdatedAt().getSeconds()));
        return entity;
    }
}
