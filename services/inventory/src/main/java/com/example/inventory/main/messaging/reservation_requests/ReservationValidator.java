package com.example.inventory.main.messaging.reservation_requests;

import com.example.inventory.main.messaging.reservation_requests.exceptions.InvalidRequestTimestamp;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class ReservationValidator {

    public void checkRequestTimestamp(ProductReservation reservation, Reservation request) throws InvalidRequestTimestamp {
        if (reservation.getUpdatedAt() != null && reservation.getUpdatedAt().isAfter(request.getRequestedAt())) {
            throw new InvalidRequestTimestamp();
        }
    }
}
