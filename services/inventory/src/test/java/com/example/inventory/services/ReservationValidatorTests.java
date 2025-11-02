package com.example.inventory.services;

import com.example.inventory.TestBase;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.services.product_reservations.ReservationRequest;
import com.example.inventory.services.product_reservations.ReservationValidator;
import com.example.inventory.services.product_reservations.exceptions.InvalidRequestTimestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReservationValidatorTests extends TestBase {

    @Test
    void ifInvalidTimestamp_throwException() {
        var reservation = new ProductReservation();
        var request = new ReservationRequest();
        var validator = new ReservationValidator();

        var now = Instant.now();
        request.setRequestedAt(now);
        reservation.setUpdatedAt(now.plusSeconds(10));

        assertThrows(InvalidRequestTimestamp.class, () ->
            validator.checkRequestTimestamp(reservation, request)
        );
    }

    @Test
    void ifValidTimestamp_doNotThrowException() {
        var reservation = new ProductReservation();
        var request = new ReservationRequest();
        var validator = new ReservationValidator();

        var now = Instant.now();
        request.setRequestedAt(now.plusSeconds(10));
        reservation.setUpdatedAt(now);

        assertDoesNotThrow(() -> validator.checkRequestTimestamp(reservation, request));
    }
}
