package com.example.inventory.services.product_reservations;

import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.services.product_reservations.exceptions.InsufficientStockForReservation;
import com.example.inventory.services.product_reservations.exceptions.InvalidRequestTimestamp;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class ReservationValidator {

    public void checkRequestTimestamp(ProductReservation reservation, ReservationRequest request) throws InvalidRequestTimestamp {
        if (reservation.getUpdatedAt().isAfter(request.getRequestedAt())) {
            throw new InvalidRequestTimestamp();
        }
    }

    public void checkStockSufficientForReservation(ProductAvailability productAvailability, ReservationRequest request) throws InsufficientStockForReservation {
        var reserveAmount = productAvailability.getReserved() + request.quantity;
        var stock = productAvailability.getStock();
        if (reserveAmount > stock) {
            throw new InsufficientStockForReservation();
        }
    }
}
