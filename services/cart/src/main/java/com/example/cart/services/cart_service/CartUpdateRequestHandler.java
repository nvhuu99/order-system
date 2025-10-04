package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.cart_service.exceptions.InvalidCartUpdateRequestVersion;
import com.example.cart.services.cart_service.exceptions.LockReleaseFailure;
import com.example.cart.services.cart_service.exceptions.SkippedUserId;
import com.example.cart.services.inventory_service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.example.cart.utils.ErrorUtils.*;

@Component
public class CartUpdateRequestHandler {

    private final Logger log = LoggerFactory.getLogger(CartUpdateRequestHandler.class);

    private final ConcurrentMap<String, Integer> observedCartVersions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> requestsHandledTotal = new ConcurrentHashMap<>();
    public Map<String, Integer> getObservedCartVersions() { return new HashMap<>(observedCartVersions); }
    public Map<String, Integer> getRequestsHandledTotal() { return new HashMap<>(requestsHandledTotal); }

    private final Set<String> skippedUserIds = ConcurrentHashMap.newKeySet();

    @Value("${order-processing-system.handlers.cart-update-requests.timeout-sec}")
    private Long timeoutSeconds;

    @Value("${order-processing-system.handlers.cart-update-requests.wait-sec}")
    private Long waitSeconds;

    @Autowired
    private LockRepository cartLockRepo;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CartValidator cartValidator;

    public Mono<Void> handle(CartUpdateRequest request, Runnable commitRequest, Runnable onSaved) {

        log.info(logTemplate(request, "Handling cart update request"));

        var lockValue = UUID.randomUUID().toString();
        var acquireLock = acquireLock(request, lockValue, Duration.ofSeconds(timeoutSeconds));
        var releaseLock = releaseLock(request, lockValue);
        var markUserIdAsSkipped = Mono.fromRunnable(() -> skippedUserIds.add(request.getUserId()));
        var commit = Mono.fromRunnable(() -> {
            if (commitRequest != null) {
                commitRequest.run();
            }
        });

        return skipIfMarked(request)
            .then(acquireLock)
            .then(updateCart(request))
            .then(releaseLock.then(commit))
            .onErrorResume(
                SkippedUserId.class, ex -> commit.then(Mono.empty())
            )
            .onErrorResume(
                LockUnavailable.class, ex -> markUserIdAsSkipped.then(commit).then(Mono.empty())
            )
            .onErrorResume(
                InvalidCartUpdateRequestVersion.class, ex -> markUserIdAsSkipped.then(releaseLock).then(commit).then(Mono.empty())
            )
            .onErrorResume(
                ex -> !(ex instanceof LockReleaseFailure), ex -> releaseLock.then(Mono.error(ex))
            )
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(cart -> {
                log.info(logTemplate(request, "Handle cart update request successfully"));
                if (onSaved != null) {
                    onSaved.run();
                }
            })
            .then()
        ;
    }

    private Mono<Void> skipIfMarked(CartUpdateRequest request) {
        return Mono
            .fromRunnable(() -> {
                if (skippedUserIds.contains(request.getUserId())) {
                    log.info(logTemplate(request, "UserId marked as skipped"));
                    throw new SkippedUserId();
                }
            })
        ;
    }

    private Mono<Void> acquireLock(CartUpdateRequest request, String lockValue, Duration lockTTL) {
        return Mono
            .defer(() -> cartLockRepo.acquireLock("carts:" + request.getUserId(), lockValue, lockTTL))
            .retryWhen(weakRetrySpec().filter(ex -> !(ex instanceof LockUnavailable)))
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Lock acquire failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Lock acquire success")))
        ;
    }

    private Mono<Void> releaseLock(CartUpdateRequest request, String lockValue) {
        return Mono
            .defer(() -> cartLockRepo.releaseLock("carts:" + request.getUserId(), lockValue))
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Lock release failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Lock release success")))
            .onErrorMap(ex -> new LockReleaseFailure())
        ;
    }

    private Mono<Cart> updateCart(CartUpdateRequest request) {
        return Mono
            .zip(
                getCart(request).defaultIfEmpty(new Cart(request.getUserId())),
                listProductAvailabilities(request).onErrorReturn(Collections.emptyList())
            )
            .flatMap(tuple2 -> buildAndValidateCart(tuple2.getT1(), tuple2.getT2(), request))
            .flatMap(cart -> saveCart(request, cart))
            .doOnSuccess(cart -> {
                observedCartVersions.put(cart.getUserId(), cart.getVersionNumber());
                requestsHandledTotal.merge(cart.getUserId(), 1, Integer::sum);
            });
    }

    private Mono<Cart> getCart(CartUpdateRequest request) {
        return Mono
            .defer(() -> cartRepo.getCartByUserId(request.getUserId()))
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Failed to get cart: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(cart ->
                log.debug(logTemplate(request, cart == null || cart.getVersionNumber() == 0
                    ? "Cart not found"
                    : "Cart found"
            )))
        ;
    }

    private Mono<List<ProductAvailability>> listProductAvailabilities(CartUpdateRequest request) {
        return Mono
            .defer(() -> inventoryService.listProductAvailabilities(request.getProductIds()).collectList())
            .retryWhen(weakRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Get product infos failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Get product infos success - IDs: {}"), request.getProductIds()))
        ;
    }

    private Mono<Cart> saveCart(CartUpdateRequest request, Cart cart) {
        return Mono
            .defer(() -> cartRepo.saveCart(cart))
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Saved cart failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Saved cart success")))
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
            .doOnError(ex -> log.error(logTemplate(request, "Build cart failed: {}"), ex.getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Build cart success")))
        ;
    }

    private RetryBackoffSpec exponentialRetrySpec() {
        return Retry.backoff(10, Duration.ofMillis(200)).jitter(0.5).doBeforeRetry(retrySignal -> {
            log.debug("Retry attempt ({}): {}", retrySignal.totalRetries() + 1, retrySignal.failure().toString());
        });
    }

    private RetryBackoffSpec weakRetrySpec() {
        return Retry.fixedDelay(3, Duration.ofMillis(100)).doBeforeRetry(retrySignal -> {
            log.debug("Weak retry attempt ({}): {}", retrySignal.totalRetries() + 1, retrySignal.failure().toString());
        });
    }

    private String logTemplate(CartUpdateRequest request, String append) {
        return String.format(
            "user_id=%s - cart_ver=%s - handler=%s - " + append,
            request.getUserId(),
            request.getVersionNumber(),
            request.getHandlerName()
        );
    }
}
