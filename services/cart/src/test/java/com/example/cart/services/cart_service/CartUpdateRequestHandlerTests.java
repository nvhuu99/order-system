package com.example.cart.services.cart_service;

import com.example.cart.TestBase;
import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.entities.properties.CartValidation.*;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.repositories.lock_repo.exceptions.LockValueMismatch;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class CartUpdateRequestHandlerTests extends TestBase {

    @Autowired
    private CartUpdateRequestHandler handler;

    @Autowired
    private LockRepository lockRepo;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private Map<String, ProductAvailability> productAvailabilities;

    private CartUpdateRequest buildRequest(String usrId) {
        return new CartUpdateRequest(usrId, 1, List.of(
            new CartUpdateRequest.CartUpdateRequestEntry("PRODUCT_001", "Product_1", 2, CartUpdateRequest.CartAction.QTY_CHANGE),
            new CartUpdateRequest.CartUpdateRequestEntry("PRODUCT_002", "Product_2", 3, CartUpdateRequest.CartAction.QTY_CHANGE)
        ), "unknown");
    }

    @Test
    void beforeAcquireLock_whenRequestVersionMismatch_thenCommitRequestWithoutSave() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);
        var lockAcquired = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);
        var cart = new Cart();

        request.setVersionNumber(3);
        cart.setVersionNumber(1);
        cart.setUserId(userId);

        cartRepo
            .saveCart(cart)
            .then(handler.handle(request, (hookName) -> {
                switch (hookName) {
                    case "LOCK_ACQUIRED" -> lockAcquired.set(true);
                    case "REQUEST_COMMITTED" -> commited.set(true);
                    case "CART_SAVED" -> saved.set(true);
                }
            }))
            .onErrorResume(ex -> Mono.empty())
            .block()
        ;

        assertFalse(lockAcquired.get());
        assertFalse(saved.get());
        assertTrue(commited.get());
    }

    @Test
    void afterAcquiredLock_whenRequestVersionMismatch_thenCommitRequestWithoutSave() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);
        var lockAcquired = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);
        var cart = new Cart();

        // before acquire lock, set use a "valid" request version
        request.setVersionNumber(2);
        cart.setVersionNumber(1);
        cart.setUserId(userId);

        cartRepo
            .saveCart(cart)
            .then(handler.handle(request, (hookName) -> {
                // After lock acquired, set an "invalid" request version
                // in order to simulate a race-condition that leads to version mismatch.
                // The handler is expected to validate the version again.
                if (hookName == "LOCK_ACQUIRED") {
                    request.setVersionNumber(3);
                    lockAcquired.set(true);
                }
                if (hookName == "REQUEST_COMMITTED") {
                    commited.set(true);
                }
                if (hookName == "CART_SAVED") {
                    saved.set(true);
                }
            }))
            .onErrorResume(ex -> Mono.empty())
            .block()
        ;

        assertTrue(lockAcquired.get());
        assertTrue(commited.get());
        assertFalse(saved.get());
    }

    @Test
    void whenLockUnavailable_thenCommitRequestWithoutSave() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);
        var lockAcquired = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        var acquireLockAndCallHandler = lockRepo
            .acquireLock("carts:" + userId, "lockValue", Duration.ofSeconds(10))
            .then(handler.handle(request, (hookName) -> {
                switch (hookName) {
                    case "LOCK_ACQUIRED" -> lockAcquired.set(true);
                    case "REQUEST_COMMITTED" -> commited.set(true);
                    case "CART_SAVED" -> saved.set(true);
                }
            }))
        ;

        assertThrows(LockUnavailable.class, acquireLockAndCallHandler::block);
        assertFalse(lockAcquired.get());
        assertFalse(saved.get());
        assertTrue(commited.get());
    }

    @Test
    void whenUnhandledErrorOccurred_mustNotCommitRequest() {
        var lockAcquired = new AtomicBoolean(false);
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        handler
            .handle(request, (hookName) -> {
                switch (hookName) {
                    case "LOCK_ACQUIRED" -> lockAcquired.set(true);
                    case "CART_SAVED" -> saved.set(true);
                    case "REQUEST_COMMITTED" -> commited.set(true);
                    case "CART_BUILT" -> throw new RuntimeException("Unhandled Error");
                }
            })
            .onErrorResume(ex -> Mono.empty())
            .block()
        ;

        assertTrue(lockAcquired.get());
        assertFalse(commited.get());
        assertFalse(saved.get());
    }

    @Test
    void whenLockReleaseFailed_mustNotCommitRequest() {
        var lockAcquired = new AtomicBoolean(false);
        var lockRelease = new AtomicBoolean(false);
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        var execute = handler
            .handle(request, (hookName) -> {
                switch (hookName) {
                    case "LOCK_ACQUIRED": {
                        lockAcquired.set(true);
                        break;
                    }
                    case "LOCK_RELEASED": {
                        lockRelease.set(true);
                        break;
                    }
                    case "REQUEST_COMMITTED": {
                        commited.set(true);
                        break;
                    }
                    case "CART_SAVED": {
                        saved.set(true);
                        break;
                    }
                    case "CART_BUILT": {
                        try {
                            lockRepo
                                .releaseLockUnSafe("carts:" + userId)
                                .then(lockRepo.acquireLock("carts:" + userId, "lockValue", Duration.ofSeconds(10)))
                                .subscribe()
                            ;
                            Thread.sleep(300);
                        } catch (Exception ignored) {
                        }
                    }
                }
            })
        ;

        assertThrows(LockValueMismatch.class, execute::block);
        assertTrue(lockAcquired.get());
        assertTrue(saved.get());
        assertFalse(lockRelease.get());
        assertFalse(commited.get());
    }

    @Test
    void whenUnhandledErrorOccurred_lockIsReleased() {
        var lockAcquired = new AtomicBoolean(false);
        var lockReleased = new AtomicBoolean(false);
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        handler
            .handle(request, (hookName) -> {
                switch (hookName) {
                    case "LOCK_ACQUIRED" -> lockAcquired.set(true);
                    case "LOCK_RELEASED" -> lockReleased.set(true);
                    case "CART_SAVED" -> saved.set(true);
                    case "REQUEST_COMMITTED" -> commited.set(true);
                    case "CART_BUILT" -> throw new RuntimeException("Unhandled Error");
                }
            })
            .onErrorResume(ex -> Mono.empty())
            .block()
        ;

        assertTrue(lockAcquired.get());
        assertTrue(lockReleased.get());
        assertFalse(commited.get());
        assertFalse(saved.get());
    }

    @Test
    void whenCartNotExist_thenPutNewCart() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        var cartAfterSaved = handler
            .handle(request, (hookName) -> {
                switch (hookName) {
                    case "CART_SAVED" -> saved.set(true);
                    case "REQUEST_COMMITTED" -> commited.set(true);
                }
            })
            .then(cartRepo.getCartByUserId(userId))
            .block();

        assertTrue(commited.get());
        assertTrue(saved.get());
        assertNotNull(cartAfterSaved);
        assertEquals(userId, cartAfterSaved.getUserId());
        assertEquals(
            Map.of(
                "prod_1_id", request.getEntries().get(0).getProductId(),
                "prod_1_name", request.getEntries().get(0).getProductName(),
                "prod_1_qty", request.getEntries().get(0).getQtyAdjustment(),

                "prod_2_id", request.getEntries().get(1).getProductId(),
                "prod_2_name", request.getEntries().get(1).getProductName(),
                "prod_2_qty", request.getEntries().get(1).getQtyAdjustment()
            ),
            Map.of(
                "prod_1_id", cartAfterSaved.getItems().get("PRODUCT_001").getProductId(),
                "prod_1_name", cartAfterSaved.getItems().get("PRODUCT_001").getProductName(),
                "prod_1_qty", cartAfterSaved.getItems().get("PRODUCT_001").getQuantity(),

                "prod_2_id", cartAfterSaved.getItems().get("PRODUCT_002").getProductId(),
                "prod_2_name", cartAfterSaved.getItems().get("PRODUCT_002").getProductName(),
                "prod_2_qty", cartAfterSaved.getItems().get("PRODUCT_002").getQuantity()
            )
        );
    }

    @Test
    void whenCartExists_thenUpdateCart() {
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var cart = new Cart(userId);
        var request = buildRequest(userId);

        cart.setProductAvailabilities(productAvailabilities);
        cart.setVersionNumber(1);
        request.setVersionNumber(2);

        var saveCartThenRequestUpdate = cartRepo
            .saveCart(cart)
            .then(handler.handle(request, (hookName) -> {
                switch (hookName) {
                    case "CART_SAVED" -> saved.set(true);
                    case "REQUEST_COMMITTED" -> commited.set(true);
                }
            }))
            .then(cartRepo.getCartByUserId(userId))
        ;
        var cartAfterSaved = saveCartThenRequestUpdate.block();

        assertTrue(commited.get());
        assertTrue(saved.get());
        assertNotNull(cartAfterSaved);
        assertEquals(userId, cartAfterSaved.getUserId());
        assertEquals(
            Map.of(
                "prod_1_id", request.getEntries().get(0).getProductId(),
                "prod_1_name", request.getEntries().get(0).getProductName(),
                "prod_1_qty", request.getEntries().get(0).getQtyAdjustment(),

                "prod_2_id", request.getEntries().get(1).getProductId(),
                "prod_2_name", request.getEntries().get(1).getProductName(),
                "prod_2_qty", request.getEntries().get(1).getQtyAdjustment()
            ),
            Map.of(
                "prod_1_id", cartAfterSaved.getItems().get("PRODUCT_001").getProductId(),
                "prod_1_name", cartAfterSaved.getItems().get("PRODUCT_001").getProductName(),
                "prod_1_qty", cartAfterSaved.getItems().get("PRODUCT_001").getQuantity(),

                "prod_2_id", cartAfterSaved.getItems().get("PRODUCT_002").getProductId(),
                "prod_2_name", cartAfterSaved.getItems().get("PRODUCT_002").getProductName(),
                "prod_2_qty", cartAfterSaved.getItems().get("PRODUCT_002").getQuantity()
            )
        );
    }

    @Test
    void afterSavedCart_alsoSetValidations() {
        var userId = UUID.randomUUID().toString();
        var cart = new Cart(userId);
        var request = buildRequest(userId);

        cart.setProductAvailabilities(productAvailabilities);
        cart.setVersionNumber(1);
        request.setVersionNumber(2);

        var saveCartThenRequestUpdate = cartRepo
            .saveCart(cart)
            .then(handler.handle(request, null))
            .then(cartRepo.getCartByUserId(userId))
        ;
        var saved = saveCartThenRequestUpdate.block();

        assertNotNull(saved);
        assertEquals(CartValidationType.PRODUCT_UNAVAILABLE, saved.getValidations().get("PRODUCT_001").getType());
        assertEquals(CartValidationType.OUT_OF_STOCK, saved.getValidations().get("PRODUCT_002").getType());
    }

    @Test
    void afterSavedCart_lockIsReleased() {
        var lockAcquired = new AtomicBoolean(false);
        var lockReleased = new AtomicBoolean(false);
        var commited = new AtomicBoolean(false);
        var saved = new AtomicBoolean(false);

        var userId = UUID.randomUUID().toString();
        var request = buildRequest(userId);

        handler
            .handle(request, (hookName) -> {
                switch (hookName) {
                    case "LOCK_ACQUIRED" -> lockAcquired.set(true);
                    case "LOCK_RELEASED" -> lockReleased.set(true);
                    case "CART_SAVED" -> saved.set(true);
                    case "REQUEST_COMMITTED" -> commited.set(true);
                }
            })
            .onErrorResume(ex -> Mono.empty())
            .block()
        ;

        assertTrue(lockAcquired.get());
        assertTrue(lockReleased.get());
        assertTrue(commited.get());
        assertTrue(saved.get());
    }
}
