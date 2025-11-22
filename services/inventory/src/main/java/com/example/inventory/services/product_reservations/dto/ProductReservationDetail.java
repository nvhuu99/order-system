package com.example.inventory.services.product_reservations.dto;

import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductReservationDetail {

    String id;

    String userId;

    String productId;

    Integer reservedAmount;

    Integer desiredAmount;

    String status;

    Instant expiresAt;

    Instant requestedAt;

    public static ProductReservationDetail mapFromEntity(ProductReservation entity) {
        var detail = new ProductReservationDetail();
        detail.setId(entity.getId());
        detail.setProductId(entity.getProductId());
        detail.setUserId(entity.getUserId());
        detail.setReservedAmount(entity.getReservedAmount());
        detail.setDesiredAmount(entity.getDesiredAmount());
        detail.setStatus(entity.getStatus());
        detail.setExpiresAt(entity.getExpiresAt());
        detail.setRequestedAt(entity.getRequestedAt());
        return detail;
    }
}
