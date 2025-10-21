package com.example.cart.services.cart_service;

import com.example.cart.entities.Cart;
import com.example.cart.entities.ProductAvailability;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.repositories.cart_repo.CartRepository;
import com.example.cart.repositories.lock_repo.LockRepository;
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

import static com.example.cart.utils.ErrorUtils.*;

@Component
public class CartUpdateRequestHandler {

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

    public Mono<Void> handle(CartUpdateRequest request, Runnable commitRequest, Runnable onSaved) {

        log.info(logTemplate(request, "Handling cart update request"));

        var commit = Mono.fromRunnable(() -> {
            if (commitRequest != null) {
                commitRequest.run();
            }
        });

        return acquireLock(request)
            .then(updateCart(request))
            .then(commit)
            .onErrorResume(LockUnavailable.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(InvalidCartUpdateRequestVersion.class, ex -> commit.then(Mono.error(ex)))
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

    private Mono<Void> acquireLock(CartUpdateRequest request) {

        return Mono
            .defer(() -> lockRepo.acquireLock(hostname, "carts:" + request.getUserId(), 0L).then())
            .retryWhen(weakRetrySpec().filter(ex -> !(ex instanceof LockUnavailable)))
            .timeout(Duration.ofSeconds(waitSeconds))
            .doOnError(ex -> log.error(logTemplate(request, "Lock acquire failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Lock acquire success")))
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
