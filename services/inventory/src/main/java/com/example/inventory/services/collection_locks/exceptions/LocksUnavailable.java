package com.example.inventory.services.collection_locks.exceptions;

public class LocksUnavailable extends RuntimeException {

    private final String collectionName;

    public LocksUnavailable(String collectionName) {
        super("lock unavailable - collection: " + collectionName);
        this.collectionName = collectionName;
    }

    public String getCollectionName() {
        return collectionName;
    }
}