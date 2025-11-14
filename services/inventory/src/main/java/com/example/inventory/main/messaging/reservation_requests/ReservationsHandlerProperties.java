package com.example.inventory.main.messaging.reservation_requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.function.BiConsumer;

public class ReservationsHandlerProperties {

    public static final String LOCK_ACQUIRED = "LOCK_ACQUIRED";
    public static final String LOCK_RELEASED = "LOCK_RELEASED";
    public static final String RESERVATION_SAVED = "RESERVATION_SAVED";
    public static final String PRODUCT_AVAILABILITY_SAVED = "PRODUCT_AVAILABILITY_SAVED";
    public static final String REQUEST_COMMITTED = "REQUEST_COMMITTED";

    protected final Logger log = LoggerFactory.getLogger(ReservationsHandler.class);

    @Value("${HOSTNAME:inventory-service}")
    protected String HOSTNAME;

    @Value("${order-system.messaging.product-reservation-requests.timeout-seconds}")
    protected Long TIMEOUT_SECONDS;


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

    protected String logTemplate(ReservationRequest request, String append) {
        return String.format(
            "product_id=%s - user_id=%s - handler=%s - " + append,
            request.getProductId(),
            request.getUserId(),
            HOSTNAME
        );
    }

    protected RetryBackoffSpec fixedDelayRetrySpec() {
        var delayMs = 50;
        var maxAttempt = (TIMEOUT_SECONDS * 1000)/delayMs;
        return Retry.fixedDelay(maxAttempt, Duration.ofMillis(delayMs)).doBeforeRetry(retrySignal ->
            log.debug("retry attempt ({}): {}", retrySignal.totalRetries() + 1, retrySignal.failure().toString())
        );
    }
}
