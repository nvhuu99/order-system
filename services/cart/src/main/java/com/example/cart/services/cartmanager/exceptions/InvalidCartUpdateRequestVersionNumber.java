package com.example.cart.services.cartmanager.exceptions;

public class InvalidCartUpdateRequestVersionNumber extends RuntimeException {
    public InvalidCartUpdateRequestVersionNumber(Integer requestVersion, Integer cartVersion) {
        super(String.format("Cart update request version (%s) must be exactly one unit ahead the cart current version (%s)",requestVersion, cartVersion));
    }
}
