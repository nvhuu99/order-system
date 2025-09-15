package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.services.cart_service.entities.CartRequest;
import com.example.cart.entities.properties.CartValidation;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class CartBuilder {

    private Cart cart;
    private CartRequest cartRequest;
    private List<ProductAvailability> productAvailabilities;
    private List<CartValidation> cartValidations;

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

    public CartBuilder setCartRequest(CartRequest request) {
        this.cartRequest = request;
        return this;
    }

    public CartBuilder build() {
        if (cartRequest != null) {
            cart.setVersionNumber(cartRequest.getVersionNumber());
            applyRequestEntries();
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

    private void applyRequestEntries() {
        var items = cart.getItems();
        cartRequest.getEntries().forEach(entry -> {
            var prodId = entry.getProductId();
            if (! items.containsKey(prodId)) {
                items.put(prodId, new Cart.CartItem(prodId, entry.getProductName(), 0));
            }
            switch (entry.getAction()) {
                case QTY_CHANGE -> items.get(prodId).setQuantity(entry.getQtyAdjustment());
                case DROP_ITEM -> items.remove(prodId);
            }
        });
    }
}
