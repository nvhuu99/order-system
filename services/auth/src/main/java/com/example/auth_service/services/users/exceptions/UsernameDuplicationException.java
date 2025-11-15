package com.example.auth_service.services.users.exceptions;

public class UsernameDuplicationException extends Exception {
    public UsernameDuplicationException() { super("Username has already existed"); }
}
