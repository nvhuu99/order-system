package com.example.inventory.services.collection_locks.exceptions;

public class LocksUnavailable extends RuntimeException {
    public LocksUnavailable(String collectionName) {
        super("lock unavailable - collection: " + collectionName);
    }
}