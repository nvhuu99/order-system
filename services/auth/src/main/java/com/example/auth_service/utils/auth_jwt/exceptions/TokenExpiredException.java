package com.example.auth_service.utils.auth_jwt.exceptions;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token expired");
    }
}
