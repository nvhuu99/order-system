package com.example.cart.services.cartmanager.internal;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import com.example.cart.entities.properties.CartValidation;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class CartBuilder {

    private Cart cart;

    private List<ProductAvailability> productAvailabilities;

    private List<CartValidation> cartValidations;

    private CartUpdateRequest cartUpdateRequest;

    public CartBuilder setCart(Cart cart) {
        this.cart = cart;
        return this;
    }

    public CartBuilder setProductAvailabilities(List<ProductAvailability> productAvailabilities) {
        this.productAvailabilities = productAvailabilities;
        return this;
    }

    public CartBuilder setCartValidations(List<CartValidation> cartValidations) {
        this.cartValidations = cartValidations;
        return this;
    }

    public CartBuilder setCartUpdateRequest(CartUpdateRequest request) {
        this.cartUpdateRequest = request;
        return this;
    }

    public CartBuilder build() {
        if (cartUpdateRequest != null) {
            cart.setVersionNumber(cartUpdateRequest.getVersionNumber());
            var items = cart.getItems();
            cartUpdateRequest.getEntries().forEach(entry -> {
                var prodId = entry.getProductId();
                var currentQty = items.get(prodId).getQuantity();
                if (! items.containsKey(prodId)) {
                    items.put(prodId, new Cart.CartItem(prodId, entry.getProductName(), 0));
                }
                var adjustment = switch (entry.getAction()) {
                    case DECREASE -> currentQty - entry.getQtyAdjustment();
                    case INCREASE -> currentQty + entry.getQtyAdjustment();
                    default -> currentQty;
                };
                if (adjustment <= 0) {
                    items.remove(prodId);
                } else {
                    items.get(prodId).setQuantity(adjustment);
                }
            });
        }

        if (productAvailabilities != null) {
            cart.getProductAvailabilities().putAll(
                productAvailabilities.stream().collect(Collectors.toMap(ProductAvailability::getProductId, p -> p))
            );
        }

        if (cartValidations != null) {
            cart.setValidations(
                cartValidations.stream().collect(Collectors.toMap(CartValidation::getProductId, v -> v))
            );
        }

        return this;
    }
}
