package com.example.auth_service.main.api.exceptions;

import jakarta.servlet.ServletException;

public class AuthenticationFailureException extends ServletException {
    public AuthenticationFailureException() { super("Authentication failure"); }
}
