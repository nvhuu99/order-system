package com.example.inventory.main.messaging.reservation_requests;

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
public class ReservationRequest {

    String requestId;
    String productId;
    String userId;
    Integer quantity;
    Instant requestedAt;

    public String getRequestLockId() { return requestId + productId; }
    public String getReservationLockId() { return productId; }
    public String getProductAvailabilityLockId() { return productId; }
}
