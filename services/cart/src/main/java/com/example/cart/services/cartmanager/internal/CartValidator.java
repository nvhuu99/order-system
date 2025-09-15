package com.example.cart.services.cartmanager.internal;

import com.example.cart.entities.Cart;
import com.example.cart.entities.properties.CartValidation;
import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import com.example.cart.services.cartmanager.exceptions.InvalidCartUpdateRequestVersion;

import java.util.List;

public interface CartValidator {
    void validateCartUpdateRequest(Cart cart, CartUpdateRequest request) throws InvalidCartUpdateRequestVersion;
    List<CartValidation> validateCart(Cart cart);
}
