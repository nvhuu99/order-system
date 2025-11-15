package com.example.auth_service.utils.exceptions;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token expired");
    }
}
