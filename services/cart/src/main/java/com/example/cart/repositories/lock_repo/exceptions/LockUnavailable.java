package com.example.cart.repositories.lock_repo.exceptions;

public class LockUnavailable extends RuntimeException {
    public LockUnavailable(String owner, String resource) {
        super(String.format(
            "Lock acquire failed due to lock unavailable. Owner: %s. Resource: %s", owner, resource));
    }
}