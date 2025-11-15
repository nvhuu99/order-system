package com.example.auth_service.services.users.exceptions;

public class EmailDuplicationException extends Exception {
    public EmailDuplicationException() { super("Email has already taken"); }
}
