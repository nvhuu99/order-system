package com.example.inventory.main.messaging.reservation_requests.exceptions;

public class RequestHasAlreadyHandled extends RuntimeException {
    public RequestHasAlreadyHandled() {
        super("request has already been handled");
    }
}
