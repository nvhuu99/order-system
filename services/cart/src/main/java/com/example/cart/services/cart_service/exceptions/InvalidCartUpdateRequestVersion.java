package com.example.cart.services.cart_service.exceptions;

public class InvalidCartUpdateRequestVersion extends RuntimeException {
    public InvalidCartUpdateRequestVersion(Integer requestVersion, Integer cartVersion) {
        super(String.format("Cart update request version (%s) must be one unit ahead the current version (%s)",requestVersion, cartVersion));
    }
}
