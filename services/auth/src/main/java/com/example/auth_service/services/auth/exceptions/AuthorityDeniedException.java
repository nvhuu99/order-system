package com.example.auth_service.services.auth.exceptions;

public class AuthorityDeniedException extends RuntimeException {
    public AuthorityDeniedException(String resourcePrefix) {
        super(String.format("access denied for resource \"%s\"", resourcePrefix));
    }
}
