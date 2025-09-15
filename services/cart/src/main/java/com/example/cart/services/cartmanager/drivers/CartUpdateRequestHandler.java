package com.example.cart.services.cartmanager.drivers;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.repositories.cart_cache_repo.CartCacheRepository;
import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import com.example.cart.services.cartmanager.exceptions.CartUpdateRequestSkip;
import com.example.cart.services.cartmanager.exceptions.InvalidCartUpdateRequestVersion;
import com.example.cart.services.cartmanager.exceptions.LockReleaseFailure;
import com.example.cart.services.inventory.InventoryService;
import com.example.cart.services.cartmanager.internal.CartBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class CartUpdateRequestHandler {

    private final Logger log = LoggerFactory.getLogger(CartUpdateRequestHandler.class);

//    @Value("${order-processing-system.handlers.cart-update-requests.timeout-sec}")
    private Long timeoutSeconds = 300L;

    @Value("${order-processing-system.storages.cart-cache.prefix}")
    private String cartCachePrefix;

    @Autowired
    private LockRepository distributedLock;

    @Autowired
    private CartCacheRepository cartCache;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CartValidator cartValidator;

    private final RetryBackoffSpec exponentialRetrySpec = Retry.backoff(10, Duration.ofMillis(200)).jitter(0.5); // ~25–76 seconds before exhausted
    private final RetryBackoffSpec weakRetrySpec = Retry.fixedDelay(3, Duration.ofMillis(100));

    @KafkaListener(
        topics = "${order-processing-system.messaging.cart-update-requests.topic-name}",
        groupId = "${order-processing-system.messaging.cart-update-requests.consumer-group-name}"
    )
    public Mono<Void> handle(CartUpdateRequest request, Acknowledgment ack) {

        var lockValue = UUID.randomUUID().toString();
        var acquireLock = acquireLock(request.getUserId(), lockValue, Duration.ofSeconds(timeoutSeconds));
        var releaseLock = releaseLock(request.getUserId(), lockValue);
        var updateCart = updateCart(request);
        var acknowlege = Mono.fromCallable(() -> { ack.acknowledge(); return Mono.empty(); });

        return acquireLock
            .then(updateCart)
            .doOnError(ex -> {
                if (ex instanceof LockUnavailable) {
                    acknowlege.subscribe();
                } else if (ex instanceof InvalidCartUpdateRequestVersion) {
                    releaseLock.then(acknowlege).subscribe();
                } else {
                    releaseLock.subscribe();
                }
            })
            .doOnError(ex ->
                log.error("Cart update request failed - UserID: {} - Message: {}", request.getUserId(), ex.getMessage())
            )
            .doOnSuccess(ok -> {
                log.info("Cart update request success - UserID: {}", request.getUserId());
                ack.acknowledge();
            })
            .onErrorResume(ex -> Mono .delay(Duration.ofSeconds(1)).then(Mono.error(ex)));
    }

    private Mono<Void> acquireLock(String userId, String lockValue, Duration lockTTL) {
        var execute = distributedLock
            .acquireLock(cartCachePrefix + userId, lockValue, lockTTL)
//            .retryWhen(weakRetrySpec)
//            .timeout(Duration.ofSeconds(1))
        ;
        return Mono.defer(() -> execute)
            .doOnSuccess(ok -> log.info("Lock acquire success"))
            .onErrorMap(ex -> unwrapException(ex, LockUnavailable.class))
            .doOnError(ex -> log.warn("Lock acquire failed - Message: " + ex.getMessage()));
    }

    private Mono<Void> releaseLock(String userId, String lockValue) {
        var execute = distributedLock
            .releaseLock(cartCachePrefix + userId, lockValue)
//            .retryWhen(exponentialRetrySpec)
//            .timeout(Duration.ofSeconds(1))
        ;
        return Mono.defer(() -> execute)
            .doOnSuccess(ok -> log.info("Lock release success"))
            .doOnError(ex -> log.info("Lock release failed - Message: " + ex.getMessage()));
    }

    private Mono<Void> updateCart(CartUpdateRequest request) {
        return Mono.zip(
            getFromCacheCart(request.getUserId()).defaultIfEmpty(new Cart(request.getUserId())),
            listProductAvailabilities(request.getProductIds()).onErrorReturn(Collections.emptyList())
        )
        .flatMap(tuple2 -> {
            var afterUpdate = applyCartUpdateRequest(tuple2.getT1(), tuple2.getT2(), request);
            var afterValidate = validateCart(afterUpdate, request);
            return saveCartToCache(afterValidate);
        })
        .doOnSuccess(ok -> log.info("Cart update success"))
        .doOnError(ex -> log.warn("Cart update failed - Message: " + ex.getMessage()))
        .then();
    }

    private Mono<Cart> getFromCacheCart(String userId) {
        var execute = cartCache
            .getCartByUserId(userId)
//            .retryWhen(exponentialRetrySpec)
//            .timeout(Duration.ofSeconds(1))
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
//            .retryWhen(weakRetrySpec)
//            .timeout(Duration.ofSeconds(1))
        ;
        return Mono.defer(() -> execute)
            .doOnSuccess(ok -> log.info("Get product infos success - IDs: " + productIds))
            .doOnError(ex -> log.warn("Get product infos failed - Message: " + ex.getMessage()));
    }

    private Mono<Cart> saveCartToCache(Cart cart) {
        var execute = cartCache
            .saveCart(cart)
//            .retryWhen(exponentialRetrySpec)
//            .timeout(Duration.ofSeconds(1))
        ;
        return Mono.defer(() -> execute)
            .doOnSuccess(ok -> log.info("Saved cart to cache success"))
            .doOnError(ex -> log.warn("Saved cart to cache failed - Message: " + ex.getMessage()));
    }

    private Cart applyCartUpdateRequest(Cart cart, List<ProductAvailability> products, CartUpdateRequest request) {
        return new CartBuilder()
            .setCart(cart)
            .setProductAvailabilities(products)
            .setCartUpdateRequest(request)
            .build()
            .getCart();
    }

    private Cart validateCart(Cart cart, CartUpdateRequest request) throws InvalidCartUpdateRequestVersion {
        cartValidator.validateCartUpdateRequest(cart, request);
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
