package com.example.cart.entities.properties;

import com.example.cart.entities.Cart;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class CartValidation {

    public enum CartValidationType {
        INSUFFICIENT_STOCK,
        OUT_OF_STOCK,
        PRODUCT_UNAVAILABLE
    }

    private final String productId;
    private final String message;
    private final CartValidationType type;

    public static CartValidation outOfStock(Cart.CartItem cartItem) {
        return new CartValidation(
            cartItem.getProductId(),
            String.format("The item %s has run out of stock", cartItem.getProductName()),
            CartValidationType.OUT_OF_STOCK
        );
    }

    public static CartValidation insufficientStock(Cart cart, Cart.CartItem cartItem) {
        return new CartValidation(
            cartItem.getProductId(),
            String.format(
                "Insufficient stocks for %s, only %s are available",
                cartItem.getProductName(),
                cart.getProductAvailability(cartItem.getProductId()).getAvailableStock()
            ),
            CartValidationType.INSUFFICIENT_STOCK
        );
    }

    public static CartValidation productUnavailable(Cart.CartItem cartItem) {
        return new CartValidation(
            cartItem.getProductId(),
            String.format("%s might not exist or unavailable at the moment", cartItem.getProductName()),
            CartValidationType.PRODUCT_UNAVAILABLE
        );
    }
}
