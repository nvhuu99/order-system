package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.repositories.cart_lock_repo.exceptions.LockUnavailable;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.repositories.cart_lock_repo.CartLockRepository;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.cart_service.exceptions.InvalidCartUpdateRequestVersion;
import com.example.cart.services.inventory_service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.example.cart.utils.ErrorUtils.*;

@Component
public class CartUpdateRequestHandler {

    private final Logger log = LoggerFactory.getLogger(CartUpdateRequestHandler.class);

//    @Value("${order-processing-system.handlers.cart-update-requests.timeout-sec}")
    private Long timeoutSeconds = 45L;

//    @Value("${order-processing-system.handlers.cart-update-requests.wait-sec}")
    private Long waitSeconds = 30L;

    @Autowired
    private CartLockRepository cartLockRepo;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CartValidator cartValidator;

    @KafkaListener(
        topics = "${order-processing-system.messaging.cart-update-requests.topic-name}",
        groupId = "consumer-group-1"
    )
    public void handle(CartUpdateRequest request, Acknowledgment ack) {

        log.info("Handling request - UserID: {} - CartVersion: {}", request.getUserId(), request.getVersionNumber());

        var lockValue = UUID.randomUUID().toString();
        var acquireLock = acquireLock(request.getUserId(), lockValue, Duration.ofSeconds(timeoutSeconds));
        var releaseLock = releaseLock(request.getUserId(), lockValue);
        var acknowledge = Mono.fromRunnable(() -> {
            ack.acknowledge();
            log.debug("Acknowledged");
        });

        acquireLock
            .then(updateCart(request))
            .then(releaseLock.then(acknowledge))
            .onErrorResume(ex -> {
                if (ex instanceof LockUnavailable) {
                    return acknowledge.then(Mono.error(ex));
                } else if (ex instanceof InvalidCartUpdateRequestVersion) {
                    return releaseLock.then(acknowledge).then(Mono.error(ex));
                } else {
                    return releaseLock.then(Mono.error(ex));
                }
            })
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(ok -> log.info("Handled request - UserID: {} - CartVersion: {}", request.getUserId(), request.getVersionNumber()))
            .subscribe()
        ;
    }

    private Mono<Void> acquireLock(String userId, String lockValue, Duration lockTTL) {
        return Mono
            .defer(() -> cartLockRepo.acquireLock(userId, lockValue, lockTTL))
            .retryWhen(weakRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .onErrorMap(ex -> {
                log.error("Lock acquire failed - Message: {}", ex.getMessage());
                return unwrapException(ex, LockUnavailable.class);
            })
            .doOnSuccess(ok -> log.debug("Lock acquire success"))
        ;
    }

    private Mono<Void> releaseLock(String userId, String lockValue) {
        return Mono
            .defer(() -> cartLockRepo.releaseLock(userId, lockValue))
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .onErrorMap(ex -> {
                log.error("Lock release failed - Message: " + ex.getMessage());
                return unwrapException(ex, LockUnavailable.class);
            })
            .doOnSuccess(ok -> log.debug("Lock release success"))
        ;
    }

    private Mono<Cart> updateCart(CartUpdateRequest request) {
        return Mono
            .zip(
                getCart(request.getUserId()).defaultIfEmpty(new Cart(request.getUserId())),
                listProductAvailabilities(request.getProductIds()).onErrorReturn(Collections.emptyList())
            )
            .flatMap(tuple2 -> buildAndValidateCart(tuple2.getT1(), tuple2.getT2(), request))
            .flatMap(this::saveCart);
    }

    private Mono<Cart> getCart(String userId) {
        return Mono
            .defer(() -> cartRepo.getCartByUserId(userId))
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> logExceptionCause(ex, "Failed to get cart - Message: {}"))
            .doOnSuccess(cart -> log.debug(
                cart == null || cart.getVersionNumber() == 0
                    ? "Cart not found"
                    : "Cart found"
            ))
        ;
    }

    private Mono<List<ProductAvailability>> listProductAvailabilities(List<String> productIds) {
        return Mono
            .defer(() -> inventoryService.listProductAvailabilities(productIds).collectList())
            .retryWhen(weakRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> logExceptionCause(ex, "Get product infos failed - Message: {}"))
            .doOnSuccess(ok -> log.debug("Get product infos success - IDs: {}", productIds))
        ;
    }

    private Mono<Cart> saveCart(Cart cart) {
        return Mono
            .defer(() -> cartRepo.saveCart(cart))
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> logExceptionCause(ex, "Saved cart to cache failed - Message: {}"))
            .doOnSuccess(ok -> log.debug("Saved cart to cache success"))
        ;
    }

    private Mono<Cart> buildAndValidateCart(Cart cart, List<ProductAvailability> products, CartUpdateRequest request) {
        return Mono
            .fromCallable(() -> {
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
            .doOnError(ex -> log.error("Build cart failed - Message: {}", ex.getMessage()))
            .doOnSuccess(ok -> log.debug("Build cart success"))
        ;
    }

    private RetryBackoffSpec exponentialRetrySpec() {
        return Retry.backoff(10, Duration.ofMillis(200)).jitter(0.5).doBeforeRetry(retrySignal -> {
            log.debug("Retry attempt {} - Message: {}", retrySignal.totalRetries() + 1, retrySignal.failure().toString());
        });
    }

    private RetryBackoffSpec weakRetrySpec() {
        return Retry.fixedDelay(3, Duration.ofMillis(100)).doBeforeRetry(retrySignal -> {
            log.debug("Retry attempt {} - Message: {}", retrySignal.totalRetries() + 1, retrySignal.failure().toString());
        });
    }

    private void logExceptionCause(Throwable ex, String template) {
        var cause = ex.getCause() != null ? ex.getCause() : ex;
        log.error(template, cause.getMessage());
    }
}
