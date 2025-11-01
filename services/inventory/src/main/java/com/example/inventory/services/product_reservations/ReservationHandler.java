package com.example.inventory.services.product_reservations;

import com.example.inventory.enums.ReservationStatus;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.repositories.product_reservations.ProductReservationsRepository;
import com.example.inventory.services.product_availabilities.ProductAvailabilityService;
import com.example.inventory.services.product_reservations.exceptions.InsufficientStockForReservation;
import com.example.inventory.services.product_reservations.exceptions.InvalidRequestTimestamp;
import com.example.inventory.services.product_reservations.exceptions.RequestHandlerLockUnavailable;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static com.example.inventory.utils.ErrorUtils.exceptionCause;

@Service
public class ReservationHandler extends ReservationHandlerProperties {
    
    @Autowired
    private ProductReservationsRepository reservationRepo;

    @Autowired
    private ProductAvailabilitiesRepository productAvailabilitiesRepo;

    @Autowired
    private ProductAvailabilityService productAvailabilityService;

    @Autowired
    private ReservationValidator validator;

    @Autowired
    private RedissonReactiveClient redisson;

    public Mono<Void> handle(ReservationRequest request, BiConsumer<String, String> hook) {

        log.info(logTemplate(request, "Handling product_reservations request"));

        var handlerLock = redisson.getReadWriteLock("order_system:reservation_request_handlers:" + request.getIdentifier()).writeLock();
        var reservationLock = redisson.getReadWriteLock("order_system:reservation_request_handlers:" + request.getIdentifier()).writeLock();
        var productAvailabilityLock = redisson.getReadWriteLock("order_system:product_availabilities:" + request.getProductId()).writeLock();

        var isHandlerLocked = new AtomicBoolean(false);
        var isReservationLocked = new AtomicBoolean(false);
        var isProductAvailabilityLocked = new AtomicBoolean(false);

        var reservationRef = new AtomicReference<ProductReservation>();
        var productAvailabilityRef = new AtomicReference<ProductAvailability>();

        var commit = Mono.fromRunnable(() -> callHook(REQUEST_COMMITTED, hook));

        return
            getReservation(request, reservationRef)
            .map(reservation -> {
                validator.checkRequestTimestamp(reservation, request);
                return reservation;
            })
            .then(tryAcquireLock(request, handlerLock, isHandlerLocked, hook))
            .then(acquireLock(request, reservationLock, isReservationLocked, hook))
            .then(acquireLock(request, productAvailabilityLock, isProductAvailabilityLocked, hook))
            .then(getProductAvailability(request, productAvailabilityRef))
            .map(ignored -> {
                validator.checkRequestTimestamp(reservationRef.get(), request);
                validator.checkStockSufficientForReservation(productAvailabilityRef.get(), request);
                return Mono.empty();
            })
            .flatMap(ignored -> {
                reservationRef.get().setQuantity(request.getQuantity());
                reservationRef.get().setProductId(request.getProductId());
                reservationRef.get().setTotalReservedSnapshot(productAvailabilityRef.get().getReserved());
                reservationRef.get().setStatus(ReservationStatus.OK.getValue());
                return putReservation(request, reservationRef.get(), hook);
            })
            .onErrorResume(InsufficientStockForReservation.class, ignored -> {
                reservationRef.get().setStatus(ReservationStatus.INSUFFICIENT_STOCK.getValue());
                return putReservation(request, reservationRef.get(), hook);
            })
            .then(releaseLock(request, handlerLock, hook))
            .then(releaseLock(request, reservationLock, hook))
            .then(releaseLock(request, productAvailabilityLock, hook))
            .then(commit)
            .onErrorResume(InvalidRequestTimestamp.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(RequestHandlerLockUnavailable.class, ex -> commit.then(Mono.error(ex)))
            .doOnError(ex -> {
                if (isHandlerLocked.get()) {
                    handlerLock.unlock().subscribe();
                }
                if (isReservationLocked.get()) {
                    reservationLock.unlock().subscribe();
                }
                if (isProductAvailabilityLocked.get()) {
                    productAvailabilityLock.unlock().subscribe();
                }
            })
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnSuccess(cart -> log.info(logTemplate(request, "Handle product_reservations request successfully")))
            .then()
        ;
    }


    private Mono<ProductReservation> getReservation(
        ReservationRequest request,
        AtomicReference<ProductReservation> reservationRef
    ) {
        return reservationRepo
            .findByProductIdAndUserId(request.getProductId(), request.getUserId())
            .defaultIfEmpty(new ProductReservation())
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "get reservation failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(cart -> log.debug(logTemplate(request, "get reservation successfully")))
            .doOnSuccess(reservationRef::set)
        ;
    }

    private Mono<Void> tryAcquireLock(ReservationRequest request, RLockReactive lock, AtomicBoolean isLocked, BiConsumer<String, String> hook) {
        return lock
            .tryLock(0, TIMEOUT_SECONDS, TimeUnit.SECONDS) // no wait for lock acquisition
            .map(acquired -> {
                if (! acquired) {
                    throw new RequestHandlerLockUnavailable();
                }
                return isLocked.getAndSet(true);
            })
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "lock acquire failed - {} - {}"), lock.getName(), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock acquire success - {}"), lock.getName()))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, lock.getName(), hook))
            .then()
        ;
    }

    private Mono<Void> acquireLock(ReservationRequest request, RLockReactive lock, AtomicBoolean isLocked, BiConsumer<String, String> hook) {
        return lock
            .lock(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnSuccess(ok -> isLocked.set(true))
            .doOnError(ex -> log.error(logTemplate(request, "lock acquire failed - {} - {}"), lock.getName(), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock acquire success - {}"), lock.getName()))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, lock.getName(), hook))
        ;
    }

    private Mono<Void> releaseLock(ReservationRequest request, RLockReactive lock, BiConsumer<String, String> hook) {
        return lock
            .unlock()
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "Lock release failed - {} - {}"), lock.getName(), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "Lock release success - {}"), lock.getName()))
            .doOnSuccess(ok -> callHook(LOCK_RELEASED, lock.getName(), hook))
        ;
    }

    private Mono<ProductAvailability> getProductAvailability(
        ReservationRequest request,
        AtomicReference<ProductAvailability> producAvailabilityRef
    ) {
        return productAvailabilitiesRepo
            .findByProductId(request.getProductId())
            .switchIfEmpty(productAvailabilityService.syncWithReservations(request.getProductId()))
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "get product availability failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(cart -> log.debug(logTemplate(request, "get product availability successfully")))
        ;
    }

    private Mono<Void> putReservation(ReservationRequest request, ProductReservation reservation, BiConsumer<String, String> hook) {
        return reservationRepo
            .save(reservation)
            .retryWhen(exponentialRetrySpec())
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "put product_reservations failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(cart -> log.debug(logTemplate(request, "put product_reservations successfully")))
            .doOnSuccess(ok -> callHook(RESERVATION_SAVED, hook))
            .then()
        ;
    }
}
