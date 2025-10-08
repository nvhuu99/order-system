package com.example.cart.services.cart_service.exceptions;

public class SkippedUserId extends RuntimeException {
    public SkippedUserId() { super("UserId marked as skipped"); }
}
