package com.example.inventory.services.product_reservations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.function.BiConsumer;

public class ReservationHandlerProperties {

    public static final String LOCK_ACQUIRED = "LOCK_ACQUIRED";
    public static final String LOCK_RELEASED = "LOCK_RELEASED";
    public static final String REQUEST_COMMITTED = "REQUEST_COMMITTED";
    public static final String RESERVATION_SAVED = "RESERVATION_SAVED";

    protected final Logger log = LoggerFactory.getLogger(ReservationHandler.class);

    @Value("${HOSTNAME:inventory-service}")
    protected String HOSTNAME;

    @Value("${order-system.handlers.product-reservation-requests.timeout-sec}")
    protected Long TIMEOUT_SECONDS;

    @Value("${order-system.handlers.product-reservation-requests.wait-sec}")
    protected Long WAIT_SECONDS;

    protected void callHook(String name, String value, BiConsumer<String, String> hook) {
        if (hook != null) {
            hook.accept(name, value);
        }
    }

    protected void callHook(String name, BiConsumer<String, String> hook) {
        if (hook != null) {
            hook.accept(name, null);
        }
    }

    protected RetryBackoffSpec exponentialRetrySpec() {
        return Retry.backoff(10, Duration.ofMillis(200)).jitter(0.5).doBeforeRetry(retrySignal -> {
            log.debug("retry attempt ({}): {}", retrySignal.totalRetries() + 1, retrySignal.failure().toString());
        });
    }

    protected RetryBackoffSpec weakRetrySpec() {
        return Retry.fixedDelay(3, Duration.ofMillis(100)).doBeforeRetry(retrySignal -> {
            log.debug("weak retry attempt ({}): {}", retrySignal.totalRetries() + 1, retrySignal.failure().toString());
        });
    }

    protected String logTemplate(ReservationRequest request, String append) {
        return String.format(
            "product_id=%s - user_id=%s - handler=%s - " + append,
            request.getProductId(),
            request.getUserId(),
            HOSTNAME
        );
    }
}
