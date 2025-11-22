package com.example.shop.services.inventory_client.entities;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductReservation {

    String id;

    String userId;

    String productId;

    Integer reservedAmount;

    Integer desiredAmount;

    String status;

    Instant expiresAt;

    Instant requestedAt;
}
