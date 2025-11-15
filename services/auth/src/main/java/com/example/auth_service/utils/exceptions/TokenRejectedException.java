package com.example.auth_service.utils.exceptions;

public class TokenRejectedException extends RuntimeException {
    public TokenRejectedException() {
        super("Token rejected");
    }
}
