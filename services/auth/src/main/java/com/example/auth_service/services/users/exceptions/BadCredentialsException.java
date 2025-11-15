package com.example.auth_service.services.users.exceptions;

public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException() {
        super("Bad credentials");
    }
}
