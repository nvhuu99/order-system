package com.example.inventory.main.messaging.reservation_requests;

import com.example.inventory.main.messaging.reservation_requests.exceptions.InvalidRequestTimestamp;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class ReservationValidator {

    public void checkRequestTimestamp(ProductReservation reservation, ReservationRequest request) throws InvalidRequestTimestamp {
        if (reservation.getRequestedAt() != null && reservation.getRequestedAt().isAfter(request.getRequestedAt())) {
            throw new InvalidRequestTimestamp();
        }
    }
}
