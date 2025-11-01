package com.example.inventory.services.product_reservations;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequest {
    String productId;
    String userId;
    Integer quantity;
    Integer reservationSnapshot;
    Instant requestedAt;

    public String getIdentifier() {
        return productId + ":" + userId;
    }
}
