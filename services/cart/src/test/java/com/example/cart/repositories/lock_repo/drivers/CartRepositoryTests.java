package com.example.cart.repositories.lock_repo.drivers;

import com.example.cart.TestBase;
import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.entities.properties.CartValidation;
import com.example.cart.repositories.cart_repo.CartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CartRepositoryTests extends TestBase {

    @Autowired
    private Map<String, ProductAvailability> productAvailabilities;

    @Autowired
    private CartRepository cartRepo;

    private Cart buildCart() {
        var cart = new Cart(UUID.randomUUID().toString());
        cart.setVersionNumber(1);
        cart.setProductAvailabilities(productAvailabilities);
        cart.getItems().put("PRODUCT_001", new Cart.CartItem("PRODUCT_001", "", 1));
        cart.setValidations(Map.of("PRODUCT_001", CartValidation.productUnavailable(cart.getItems().get("PRODUCT_001"))));
        return cart;
    }

    @Test
    void saveCart_mustSuccess() {
        var cart = buildCart();
        assertEquals(cart, cartRepo.saveCart(cart).block());
    }

    @Test
    void afterSavedCart_canGetByUserId() {
        var cart = buildCart();
        var savedCart = cartRepo.saveCart(cart).then(cartRepo.getCartByUserId(cart.getUserId())).block();
        assertEquals(cart, savedCart);
    }
}
