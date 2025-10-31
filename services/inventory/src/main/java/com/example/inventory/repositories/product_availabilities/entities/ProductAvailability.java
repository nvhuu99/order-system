package com.example.inventory.repositories.product_availabilities.entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAvailability {

    String productId;

    Integer reserved;

    Integer stock;

    Instant updatedAt;
}
