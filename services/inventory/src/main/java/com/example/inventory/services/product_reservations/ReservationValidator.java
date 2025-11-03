package com.example.inventory.services.product_reservations;

import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.services.product_reservations.exceptions.InvalidRequestTimestamp;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class ReservationValidator {

    public void checkRequestTimestamp(ProductReservation reservation, ReservationRequest request) throws InvalidRequestTimestamp {
        if (reservation.getUpdatedAt() != null && reservation.getUpdatedAt().isAfter(request.getRequestedAt())) {
            throw new InvalidRequestTimestamp();
        }
    }
}
