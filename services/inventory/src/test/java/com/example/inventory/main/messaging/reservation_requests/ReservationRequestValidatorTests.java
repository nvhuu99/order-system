package com.example.inventory.main.messaging.reservation_requests;

import com.example.inventory.TestBase;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.main.messaging.reservation_requests.exceptions.InvalidRequestTimestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReservationRequestValidatorTests extends TestBase {

    @Test
    void ifInvalidTimestamp_throwException() {
        var reservation = new ProductReservation();
        var request = new ReservationRequest();
        var validator = new ReservationValidator();

        var now = Instant.now();
        reservation.setUpdatedAt(now);
        request.setRequestedAt(now.minusSeconds(10));

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
        reservation.setUpdatedAt(now);
        request.setRequestedAt(now.plusSeconds(10));

        assertDoesNotThrow(() -> validator.checkRequestTimestamp(reservation, request));
    }
}
