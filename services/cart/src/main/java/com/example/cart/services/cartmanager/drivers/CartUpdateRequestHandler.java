package com.example.cart.services.cartmanager.drivers;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.repositories.cart_cache_repo.CartCacheRepository;
import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.repositories.lock_repo.exceptions.LockAcquireFailure;
import com.example.cart.services.cartmanager.entities.CartUpdateRequest;
import com.example.cart.services.cartmanager.exceptions.InvalidCartUpdateRequestVersionNumber;
import com.example.cart.services.inventory.InventoryService;
import com.example.cart.services.cartmanager.internal.CartBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CartUpdateRequestHandler implements com.example.cart.services.cartmanager.internal.CartUpdateRequestHandler {

    @Value("${order-processing-system.cart.timeout-sec}")
    private Long timeoutSeconds;

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

    @Override
    public Mono<Void> handle(CartUpdateRequest request) {

        var lockValue = new AtomicReference<>("");
        var acquireLock = acquireLock(request.getCartId(), lockValue, Duration.ofSeconds(timeoutSeconds));
        var releaseLock = releaseLock(request.getCartId(), lockValue);
        var getCartCreateIfNotExist = getCartCreateIfNotExist(request.getCartId());
        var listProductAvailabilities = listProductAvailabilities(request.getProductIds());

        return acquireLock
            .onErrorResume(LockAcquireFailure.class, ex -> Mono.empty())
            .zip(
                getCartCreateIfNotExist,
                listProductAvailabilities.onErrorReturn(Collections.emptyList())
            )
            .flatMap(tuple2 -> {
                var afterUpdate = applyCartUpdateRequest(tuple2.getT1(), tuple2.getT2(), request);
                var afterValidate = validateCart(afterUpdate, request);
                return Mono.just(afterValidate);
            })
            .onErrorResume(InvalidCartUpdateRequestVersionNumber.class, ex -> Mono.empty())
            .flatMap(cart -> saveCartToCache(cart))
            .then(releaseLock.onErrorComplete())
            .timeout(Duration.ofSeconds(timeoutSeconds));
    }

    private Mono<Void> acquireLock(String cartId, AtomicReference<String> lockValue, Duration lockTTL) {
        return distributedLock
            .acquireLock("cart:" + cartId, lockTTL)
            .doOnSuccess(acquiredLock -> lockValue.set(acquiredLock))
            .onErrorMap(ex -> {
                if (!(ex instanceof LockAcquireFailure) &&
                    (ex.getCause() instanceof LockAcquireFailure cause)) {
                    return cause;
                }
                return ex;
            })
            .retryWhen(weakRetrySpec)
            .then();
    }

    private Mono<Void> releaseLock(String cartId, AtomicReference<String> lockValue) {
        return distributedLock
            .releaseLock("cart:" + cartId, lockValue)
            .retryWhen(exponentialRetrySpec)
            .timeout(Duration.ofSeconds(5));
    }

    private Mono<Cart> getCartCreateIfNotExist(String cartId) {
        return cartCache.getCartById(cartId)
            .defaultIfEmpty(new Cart(UUID.randomUUID().toString(), "usr_1"))
            .retryWhen(exponentialRetrySpec);
    }

    private Mono<List<ProductAvailability>> listProductAvailabilities(List<String> productIds) {
        return inventoryService.listProductAvailabilities(productIds)
            .collectList()
            .retryWhen(weakRetrySpec);
    }

    private Mono<Cart> saveCartToCache(Cart cart) {
        return cartCache.saveCart(cart).retryWhen(exponentialRetrySpec);
    }

    private Cart applyCartUpdateRequest(Cart cart, List<ProductAvailability> products, CartUpdateRequest request) {
        return new CartBuilder()
            .setCart(cart)
            .setProductAvailabilities(products)
            .setCartUpdateRequest(request)
            .getCart();
    }

    private Cart validateCart(Cart cart, CartUpdateRequest request) throws InvalidCartUpdateRequestVersionNumber {
        cartValidator.validateCartUpdateRequest(cart, request);
        var validations = cartValidator.validateCart(cart);
        return new CartBuilder()
            .setCart(cart)
            .setCartValidations(validations)
            .build()
            .getCart();
    }
}
