package com.example.auth_service.utils.auth_jwt.exceptions;

public class TokenRejectedException extends RuntimeException {
    public TokenRejectedException() {
        super("Token rejected");
    }
}
