package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.services.cart_service.entities.CartRequest;
import com.example.cart.services.cart_service.exceptions.InvalidCartRequestVersion;
import com.example.cart.services.inventory_service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class CartRequestHandler {

    private final Logger log = LoggerFactory.getLogger(CartRequestHandler.class);

//    @Value("${order-processing-system.handlers.cart-update-requests.timeout-sec}")
    private Long timeoutSeconds = 300L;

    @Autowired
    private CartRepository cartCache;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CartValidator cartValidator;

    private final RetryBackoffSpec exponentialRetrySpec = Retry.backoff(10, Duration.ofMillis(200)).jitter(0.5); // ~25–76 seconds before exhausted
    private final RetryBackoffSpec weakRetrySpec = Retry.fixedDelay(3, Duration.ofMillis(100));

    public Mono<Cart> handle(CartRequest request) {
        return Mono
            .zip(
                getCart(request.getUserId()).defaultIfEmpty(new Cart(request.getUserId())),
                listProductAvailabilities(request.getProductIds()).onErrorReturn(Collections.emptyList())
            )
            .flatMap(tuple2 -> {
                var afterUpdate = applyCartRequest(tuple2.getT1(), tuple2.getT2(), request);
                var afterValidate = validateCart(afterUpdate, request);
                return saveCartToCache(afterValidate);
            })
            .doOnSuccess(ok -> log.info("Cart update success"))
            .doOnError(ex -> log.warn("Cart update failed - Message: " + ex.getMessage()))
            .timeout(Duration.ofSeconds(timeoutSeconds));
    }

    private Mono<Cart> getCart(String userId) {
        var execute = cartCache
            .getCartByUserId(userId)
            .retryWhen(exponentialRetrySpec)
            .timeout(Duration.ofSeconds(1))
        ;
        return Mono.defer(() -> execute)
            .doOnSuccess(cart -> log.info(
                cart.getVersionNumber() == 0
                    ? "Cart not found in cache"
                    : "Cart found in cache"
            ))
            .doOnError(ex -> log.warn("Failed to get cart from cache - Message: " + ex.getMessage()));
    }

    private Mono<List<ProductAvailability>> listProductAvailabilities(List<String> productIds) {
        var execute = inventoryService
            .listProductAvailabilities(productIds)
            .collectList()
            .retryWhen(weakRetrySpec)
            .timeout(Duration.ofSeconds(1))
        ;
        return Mono.defer(() -> execute)
            .doOnSuccess(ok -> log.info("Get product infos success - IDs: " + productIds))
            .doOnError(ex -> log.warn("Get product infos failed - Message: " + ex.getMessage()));
    }

    private Mono<Cart> saveCartToCache(Cart cart) {
        var execute = cartCache
            .saveCart(cart)
            .retryWhen(exponentialRetrySpec)
            .timeout(Duration.ofSeconds(1))
        ;
        return Mono.defer(() -> execute)
            .doOnSuccess(ok -> log.info("Saved cart to cache success"))
            .doOnError(ex -> log.warn("Saved cart to cache failed - Message: " + ex.getMessage()));
    }

    private Cart applyCartRequest(Cart cart, List<ProductAvailability> products, CartRequest request) {
        return new CartBuilder()
            .setCart(cart)
            .setProductAvailabilities(products)
            .setCartRequest(request)
            .build()
            .getCart();
    }

    private Cart validateCart(Cart cart, CartRequest request) throws InvalidCartRequestVersion {
        cartValidator.validateCartRequest(cart, request);
        var validations = cartValidator.validateCart(cart);
        return new CartBuilder()
            .setCart(cart)
            .setCartValidations(validations)
            .build()
            .getCart();
    }

    private <T extends Throwable> Throwable unwrapException(Throwable rootException, Class<T> targetType) {
        if (!targetType.isInstance(rootException) && targetType.isInstance(rootException.getCause())) {
            return targetType.cast(rootException.getCause());
        }
        return rootException;
    }

    private <T extends Throwable, R extends Throwable> Throwable tryMapToException(
        Throwable rootException,
        Class<T> matchType,
        Class<R> mapType
    ) {
        try {
            return matchType.isInstance(rootException)
                ? mapType.getDeclaredConstructor().newInstance()
                : rootException;
        } catch (Exception ignored) {
            return rootException;
        }
    }
}
