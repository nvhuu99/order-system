package com.example.cart.repositories.cart_lock_repo.exceptions;

public class LockUnavailable extends RuntimeException {
    public LockUnavailable(String target) { super("Lock acquire failed due to lock unavailable, resource name: " + target); }
}