package com.example.inventory.main.messaging.reservation_requests.exceptions;

public class ProductNotFound extends RuntimeException{
    public ProductNotFound() {
        super("product not found");
    }
}
