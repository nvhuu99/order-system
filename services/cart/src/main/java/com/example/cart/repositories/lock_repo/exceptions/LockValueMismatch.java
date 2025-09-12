package com.example.cart.repositories.lock_repo.exceptions;

public class LockValueMismatch extends RuntimeException {
    public LockValueMismatch(String target) { super("Lock value mismatch. Lock release failed for:" + target); }
}
