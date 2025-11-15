package com.example.auth_service.services.auth.exceptions;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String requestedRole) {
        super(String.format("this user does not have the \"%s\" role", requestedRole));
    }
}
