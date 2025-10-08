package com.example.cart.repositories.cart_repo.exceptions;

public class SaveCartFailed extends RuntimeException {
    public SaveCartFailed() {
        super("Save cart failed");
    }
}
