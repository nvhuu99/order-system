package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.inventory_service.InventoryService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class CartUpdateRequestHandler {

    private final Logger log = LoggerFactory.getLogger(CartUpdateRequestHandler.class);

//    @Value("${order-processing-system.handlers.cart-update-requests.timeout-sec}")
    private Long timeoutSeconds = 300L;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CartValidator cartValidator;

    private final RetryBackoffSpec exponentialRetrySpec = Retry.backoff(10, Duration.ofMillis(200)).jitter(0.5); // ~25–76 seconds before exhausted
    private final RetryBackoffSpec weakRetrySpec = Retry.fixedDelay(3, Duration.ofMillis(100));

    @Observed(name = "cart_update_request_handler.handle")
    public Mono<Cart> handle(CartUpdateRequest request) {
        log.info("Handling request");
        return Mono
            .zip(
                getCart(request.getUserId()).defaultIfEmpty(new Cart(request.getUserId())),
                listProductAvailabilities(request.getProductIds()).onErrorReturn(Collections.emptyList())
            )
            .flatMap(
                tuple2 -> buildAndValidateCart(tuple2.getT1(), tuple2.getT2(), request)
            )
            .flatMap(cart -> saveCart(cart))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(ok -> log.info("Handled request"))
            .doOnError(ex -> logExceptionCause(ex, "Handle request failed - Message: {}"));
    }

    private Mono<Cart> getCart(String userId) {
        var execute = cartRepo
            .getCartByUserId(userId)
            .retryWhen(exponentialRetrySpec)
            .timeout(Duration.ofSeconds(1))
            .doOnSuccess(cart -> log.info(
                cart == null || cart.getVersionNumber() == 0
                    ? "Cart not found"
                    : "Cart found"
            ))
            .doOnError(ex -> logExceptionCause(ex, "Failed to get cart - Message: {}"))
        ;
        return Mono.defer(() -> execute);
    }

    private Mono<List<ProductAvailability>> listProductAvailabilities(List<String> productIds) {
        var execute = inventoryService
            .listProductAvailabilities(productIds)
            .collectList()
            .retryWhen(weakRetrySpec)
            .timeout(Duration.ofSeconds(1))
            .doOnSuccess(ok -> log.info("Get product infos success - IDs: {}", productIds))
            .doOnError(ex -> logExceptionCause(ex, "Get product infos failed - Message: {}"))
        ;
        return Mono.defer(() -> execute);
    }

    private Mono<Cart> saveCart(Cart cart) {
        var execute = cartRepo
            .saveCart(cart)
            .retryWhen(exponentialRetrySpec)
            .timeout(Duration.ofSeconds(1))
            .doOnSuccess(ok -> log.info("Saved cart to cache success"))
            .doOnError(ex -> logExceptionCause(ex, "Saved cart to cache failed - Message: {}"))
        ;
        return Mono.defer(() -> execute);
    }

    private Mono<Cart> buildAndValidateCart(Cart cart, List<ProductAvailability> products, CartUpdateRequest request) {
        var execute = Mono.fromCallable(() -> {
            // Validate cart version
            cartValidator.validateCartUpdateRequest(cart, request);
            // Build and validate
            return new CartBuilder()
                .setCart(cart)
                .setProductAvailabilities(products)
                .setCartUpdateRequest(request)
                .build()
                .setCartValidations(cartValidator.validateCart(cart))
                .build()
                .getCart();
        })
        .doOnSuccess(ok -> log.info("Build cart success"))
        .doOnError(ex -> log.error("Build cart failed - Message: {}", ex.getMessage()));
        return Mono.defer(() -> execute);
    }

    private void logExceptionCause(Throwable ex, String template) {
        var cause = ex.getCause() != null ? ex.getCause() : ex;
        log.error(template, cause.getMessage());
    }
}
