package com.example.shop.services.inventory.entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductReservation {

    String id;

    String userId;

    String productId;

    Integer reserved = 0;

    Integer desiredAmount = 0;

    String status;

    Instant expiredAt;

    Instant updatedAt;
}
