package com.example.inventory.services.collection_locks.exceptions;

public class LockValueMismatch extends RuntimeException {
    public LockValueMismatch(String collection) {
        super("lock value mismatch - collection: " + collection);
    }
}