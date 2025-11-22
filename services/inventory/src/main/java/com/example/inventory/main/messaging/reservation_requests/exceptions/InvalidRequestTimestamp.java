package com.example.inventory.main.messaging.reservation_requests.exceptions;

public class InvalidRequestTimestamp extends RuntimeException {
    public InvalidRequestTimestamp() {
        super("invalid request timestamp");
    }
}
