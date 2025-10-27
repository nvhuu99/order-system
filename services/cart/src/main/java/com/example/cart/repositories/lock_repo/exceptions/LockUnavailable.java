package com.example.cart.repositories.lock_repo.exceptions;

public class LockUnavailable extends RuntimeException {
    public LockUnavailable(String resource) {
        super(String.format(
            "Lock acquire failed due to lock unavailable. Resource: %s", resource));
    }
}