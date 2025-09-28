package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.entities.properties.CartValidation;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.cart_service.exceptions.InvalidCartUpdateRequestVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.*;

public class CartValidatorTests {

    private final CartValidator validator = new CartValidator();

    @Autowired
    private Map<String, ProductAvailability> productAvailabilities;

    @Test
    void validateCartUpdateRequest_mustThrowInvalidCartUpdateRequestVersion() {
        var cart = new Cart();
        var request = new CartUpdateRequest();

        cart.setVersionNumber(1);
        request.setVersionNumber(1);
        assertThrows(InvalidCartUpdateRequestVersion.class, () -> validator.validateCartUpdateRequest(cart, request));

        cart.setVersionNumber(1);
        request.setVersionNumber(3);
        assertThrows(InvalidCartUpdateRequestVersion.class, () -> validator.validateCartUpdateRequest(cart, request));

        cart.setVersionNumber(2);
        request.setVersionNumber(1);
        assertThrows(InvalidCartUpdateRequestVersion.class, () -> validator.validateCartUpdateRequest(cart, request));
    }

    @Test
    void validateCartUpdateRequest_mustBeValid() {
        var cart = new Cart();
        var request = new CartUpdateRequest();

        cart.setVersionNumber(1);
        request.setVersionNumber(2);

        assertThatNoException().isThrownBy(() -> validator.validateCartUpdateRequest(cart, request));
    }

    @Test
    void validateCart_mustReturnProductUnavailable() {
        var cart = new Cart("USR_01");
        cart.setProductAvailabilities(productAvailabilities);
        cart.getItems().put("PRODUCT_001", new Cart.CartItem("PRODUCT_001", "", 1));

        var validations = validator.validateCart(cart);
        assertNotNull(validations.getFirst());
        assertEquals(validations.getFirst().getType(), CartValidation.CartValidationType.PRODUCT_UNAVAILABLE);
    }

    @Test
    void validateCart_mustReturnOutOfStock() {
        var cart = new Cart("USR_01");
        cart.setProductAvailabilities(productAvailabilities);
        cart.getItems().put("PRODUCT_002", new Cart.CartItem("PRODUCT_002", "", 1));

        var validations = validator.validateCart(cart);
        assertNotNull(validations.getFirst());
        assertEquals(validations.getFirst().getType(), CartValidation.CartValidationType.OUT_OF_STOCK);
    }

    @Test
    void validateCart_mustReturnInSufficientStock() {
        var cart = new Cart("USR_01");
        cart.setProductAvailabilities(productAvailabilities);
        cart.getItems().put("PRODUCT_003", new Cart.CartItem("PRODUCT_003", "", 5));

        var validations = validator.validateCart(cart);
        assertNotNull(validations.getFirst());
        assertEquals(validations.getFirst().getType(), CartValidation.CartValidationType.INSUFFICIENT_STOCK);
    }

    @Test
    void validateCart_mustBeValid() {
        var cart = new Cart("USR_01");
        cart.setProductAvailabilities(productAvailabilities);
        cart.getItems().put("PRODUCT_003", new Cart.CartItem("PRODUCT_003", "", 2));

        var validations = validator.validateCart(cart);
        assertTrue(validations.isEmpty());
    }
}

