package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.repositories.lock_repo.exceptions.LockValueMismatch;
import com.example.cart.services.cart_service.entities.CartUpdateRequest;
import com.example.cart.services.cart_service.exceptions.InvalidCartUpdateRequestVersion;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.example.cart.utils.ErrorUtils.*;

@Component
public class CartUpdateRequestHandler {

    public static final String LOCK_ACQUIRED = "LOCK_ACQUIRED";
    public static final String LOCK_RELEASED = "LOCK_RELEASED";
    public static final String REQUEST_COMMITTED = "REQUEST_COMMITTED";
    public static final String CART_BUILT = "CART_BUILT";
    public static final String CART_SAVED = "CART_SAVED";

    private final Logger log = LoggerFactory.getLogger(CartUpdateRequestHandler.class);

    private final ConcurrentMap<String, Integer> observedCartVersions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> requestsHandledTotal = new ConcurrentHashMap<>();
    public Map<String, Integer> getObservedCartVersions() { return new HashMap<>(observedCartVersions); }
    public Map<String, Integer> getRequestsHandledTotal() { return new HashMap<>(requestsHandledTotal); }

    @Value("${HOSTNAME:cart-service}")
    private String hostname;

    @Value("${order-processing-system.handlers.cart-update-requests.timeout-sec}")
    private Long timeoutSeconds;

    @Value("${order-processing-system.handlers.cart-update-requests.wait-sec}")
    private Long waitSeconds;

    @Autowired
    private LockRepository lockRepo;

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CartValidator cartValidator;

    public Mono<Void> handle(CartUpdateRequest request, Consumer<String> hook) {

        log.info(logTemplate(request, "Handling cart update request"));

        var lockValue = UUID.randomUUID().toString();
        var isLocked = new AtomicBoolean(false);
        var acquireLock = acquireLock(request, lockValue, isLocked, hook);
        var releaseLock = releaseLock(request, lockValue, hook);
        var commit = Mono.fromRunnable(() -> callHook(REQUEST_COMMITTED, hook));

        return checkRequestVersion(request)
            .then(acquireLock)
            .then(updateCart(request, hook))
            .then(releaseLock.then(commit))
            .onErrorResume(InvalidCartUpdateRequestVersion.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(LockUnavailable.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(ex -> isLocked.get()
                ? releaseLock.then(Mono.error(ex))
                : Mono.error(ex)
            )
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(cart -> log.info(logTemplate(request, "Handle cart update request successfully")))
            .then()
        ;
    }

    private Mono<Void> checkRequestVersion(CartUpdateRequest request) {
        return Mono
            .defer(() -> cartRepo.getCartByUserId(request.getUserId()))
            .defaultIfEmpty(new Cart())
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .map(cart -> {
                if (cart != null && (cart.getUserId() == request.getUserId())) {
                    cartValidator.checkCartUpdateRequestVersion(cart, request);
                }
                return cart;
            })
            .doOnError(ex -> log.error(logTemplate(request, "Failed to check request version: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(cart -> log.debug(logTemplate(request, "Request version checked")))
            .then()
        ;
    }

    private Mono<Void> acquireLock(CartUpdateRequest request, String lockValue, AtomicBoolean isLocked, Consumer<String> hook) {
        return Mono
            .defer(() -> lockRepo.acquireLock("carts:" + request.getUserId(), lockValue, Duration.ofSeconds(timeoutSeconds)))
            .retryWhen(exponentialRetrySpec().filter(ex -> !(ex instanceof LockUnavailable)))
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Lock acquire failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Lock acquire success")))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, hook))
            .doOnSuccess(ok -> isLocked.set(true))
        ;
    }

    private Mono<Void> releaseLock(CartUpdateRequest request, String lockValue, Consumer<String> hook) {
        return Mono
            .defer(() -> lockRepo.releaseLock("carts:" + request.getUserId(), lockValue))
            .retryWhen(exponentialRetrySpec().filter(ex -> !(ex instanceof LockValueMismatch)))
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Lock release failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Lock release success")))
            .doOnSuccess(ok -> callHook(LOCK_RELEASED, hook))
        ;
    }

    private Mono<Cart> updateCart(CartUpdateRequest request, Consumer<String> hook) {
        return Mono
            .zip(
                getCart(request).defaultIfEmpty(new Cart(request.getUserId())),
                listProductAvailabilities(request).onErrorReturn(Collections.emptyList())
            )
            .flatMap(tuple2 -> buildAndValidateCart(tuple2.getT1(), tuple2.getT2(), request, hook))
            .flatMap(cart -> saveCart(request, cart, hook))
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

    private Mono<Cart> saveCart(CartUpdateRequest request, Cart cart, Consumer<String> hook) {
        return Mono
            .defer(() -> cartRepo.saveCart(cart))
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Saved cart failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Saved cart success")))
            .doOnSuccess(ok -> callHook(CART_SAVED, hook))
        ;
    }

    private Mono<Cart> buildAndValidateCart(Cart cart, List<ProductAvailability> products, CartUpdateRequest request, Consumer<String> hook) {
        return Mono
            .fromCallable(() -> {
                cartValidator.checkCartUpdateRequestVersion(cart, request);
                return new CartBuilder()
                    .setCart(cart)
                    .setProductAvailabilities(products)
                    .setCartUpdateRequest(request)
                    .build()
                    .setCartValidations(cartValidator.validateCartItems(cart))
                    .build()
                    .getCart();
            })
            .doOnError(ex -> log.error(logTemplate(request, "Build cart failed: {}"), ex.getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Build cart success")))
            .doOnSuccess(ok -> callHook(CART_BUILT, hook))
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

    private void callHook(String name, Consumer<String> hook) {
        if (hook != null) {
            hook.accept(name);
        }
    }
}
