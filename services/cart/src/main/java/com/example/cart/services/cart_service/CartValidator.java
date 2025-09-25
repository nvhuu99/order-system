package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.properties.CartValidation;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.cart_service.exceptions.InvalidCartUpdateRequestVersion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class CartValidator {

    public void validateCartUpdateRequest(Cart cart, CartUpdateRequest request) throws InvalidCartUpdateRequestVersion {
        if (cart.getVersionNumber() + 1 != request.getVersionNumber()) {
            throw new InvalidCartUpdateRequestVersion(request.getVersionNumber(), cart.getVersionNumber());
        }
    }

    public List<CartValidation> validateCart(Cart cart) {
        return cart.getItems().values().stream()
            .map(cartItem -> checkProductAvailabilities(cart, cartItem))
            .filter(Objects::nonNull)
            .toList();
    }

    private CartValidation checkProductAvailabilities(Cart cart, Cart.CartItem cartItem) {
        if (isUnavailable(cart, cartItem)) {
            return CartValidation.productUnavailable(cartItem);
        }
        if (isOutOfStock(cart, cartItem)) {
            return CartValidation.outOfStock(cartItem);
        }
        if (isSufficientStock(cart, cartItem)) {
            return CartValidation.insufficientStock(cart, cartItem);
        }
        return null;
    }

    private Boolean isUnavailable(Cart cart, Cart.CartItem cartItem) {
        return Optional.ofNullable(cart.getProductAvailability(cartItem.getProductId()))
            .map(prod -> !prod.getIsAvailable())
            .orElse(false);
    }

    private Boolean isOutOfStock(Cart cart, Cart.CartItem cartItem) {
        return Optional.ofNullable(cart.getProductAvailability(cartItem.getProductId()))
            .map(prod -> prod.getAvailableStock() == 0)
            .orElse(false);
    }

    private Boolean isSufficientStock(Cart cart, Cart.CartItem cartItem) {
        return Optional.ofNullable(cart.getProductAvailability(cartItem.getProductId()))
            .map(prod -> cartItem.getQuantity() > prod.getAvailableStock())
            .orElse(false);
    }
}

