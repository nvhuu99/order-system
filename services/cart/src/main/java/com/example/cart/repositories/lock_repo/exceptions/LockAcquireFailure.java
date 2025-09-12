package com.example.cart.repositories.lock_repo.exceptions;

public class LockAcquireFailure extends RuntimeException {
    public LockAcquireFailure(String target) { super("Lock acquire attempt failed for: " + target); }
}
