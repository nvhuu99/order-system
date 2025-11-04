package com.example.inventory.services.product_reservations;

import com.example.inventory.enums.ReservationStatus;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.repositories.product_reservations.ProductReservationsCrudRepository;
import com.example.inventory.services.product_availabilities.ProductAvailabilitiesService;
import com.example.inventory.services.product_reservations.exceptions.InvalidRequestTimestamp;
import com.example.inventory.services.product_reservations.exceptions.RequestHandlerLockUnavailable;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static com.example.inventory.utils.ErrorUtils.exceptionCause;

@Service
public class ReservationHandler extends ReservationHandlerProperties {
    
    @Autowired
    private ProductReservationsCrudRepository reservationRepo;

    @Autowired
    private ProductAvailabilitiesRepository productAvailabilitiesRepo;

    @Autowired
    private ProductAvailabilitiesService productAvailabilitiesService;

    @Autowired
    private ReservationValidator validator;

    @Autowired
    private RedissonReactiveClient redisson;

    public Mono<Void> handle(ReservationRequest request, BiConsumer<String, String> hook) {

        log.info(logTemplate(request, "handling reservation request"));

        var handlerLock = redisson.getReadWriteLock("order_system:reservation_request_handlers:" + request.getIdentifier()).writeLock();
        var reservationLock = redisson.getReadWriteLock("order_system:product_reservations:" + request.getIdentifier()).writeLock();
        var productAvailabilityLock = redisson.getReadWriteLock("order_system:product_availabilities:" + request.getProductId()).writeLock();

        var reservationRef = new AtomicReference<ProductReservation>();
        var productAvailabilityRef = new AtomicReference<ProductAvailability>();
        var now = Instant.now();

        var commit = Mono.fromRunnable(() -> callHook(REQUEST_COMMITTED, hook));

        return getReservation(request, reservationRef)
            .map(reservation -> {
                validator.checkRequestTimestamp(reservation, request);
                return reservation;
            })
            .then(acquireHandlerLock(request, handlerLock, hook))
            .then(acquireLock(request, reservationLock, hook))
            .then(acquireLock(request, productAvailabilityLock, hook))
            .then(getReservation(request, reservationRef))
            .then(getProductAvailability(request, productAvailabilityRef))
            .map(ignored -> {
                validator.checkRequestTimestamp(reservationRef.get(), request);
                return Mono.empty();
            })
            .map(ignored -> {
                var availability = productAvailabilityRef.get();
                var reservation = reservationRef.get();

                var reservedTotalAfterExcludeReservation = availability.getReserved() - reservation.getReserved();
                var desiredReserveTotal = reservedTotalAfterExcludeReservation + request.getQuantity();
                var maxAdditional = availability.getStock() < desiredReserveTotal
                    ? availability.getStock() - reservedTotalAfterExcludeReservation
                    : request.getQuantity()
                ;

                availability.setReserved(reservedTotalAfterExcludeReservation + maxAdditional);
                availability.setUpdatedAt(now);

                reservation.setProductId(request.getProductId());
                reservation.setReserved(maxAdditional);
                reservation.setDesiredAmount(request.getQuantity());
                if (request.getQuantity() > maxAdditional) {
                    reservation.setStatus(ReservationStatus.INSUFFICIENT_STOCK.getValue());
                } else {
                    reservation.setStatus(ReservationStatus.OK.getValue());
                }
                reservation.setExpiredAt(now.plusSeconds(EXPIRES_AFTER_SECONDS));
                reservation.setUpdatedAt(now);

                return Mono.empty();
            })
            .flatMap(ignored ->
                putReservation(request, reservationRef.get(), hook)
                    .then(putProductAvailability(request, productAvailabilityRef.get(), hook))
            )
            .then(releaseLock(request, productAvailabilityLock, hook))
            .then(releaseLock(request, reservationLock, hook))
            .then(releaseLock(request, handlerLock, hook))
            .then(commit)
            .onErrorResume(InvalidRequestTimestamp.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(RequestHandlerLockUnavailable.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(ex ->
                    releaseLock(request, productAvailabilityLock, hook)
                        .then(releaseLock(request, reservationLock, hook))
                        .then(releaseLock(request, handlerLock, hook))
                        .then(Mono.error(ex))
            )
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnSuccess(ok -> log.info(logTemplate(request, "handle reservation request successfully")))
            .then()
        ;
    }


    private Mono<ProductReservation> getReservation(
        ReservationRequest request,
        AtomicReference<ProductReservation> reservationRef
    ) {
        return reservationRepo
            .findByProductIdAndUserId(request.getProductId(), request.getUserId())
            .defaultIfEmpty(
                new ProductReservation(null, request.getUserId(), request.getProductId(), 0, 0, null, null, null)
            )
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "get reservation failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "get reservation successfully")))
            .doOnSuccess(reservationRef::set)
        ;
    }

    private Mono<Void> acquireHandlerLock(ReservationRequest request, RLockReactive lock, BiConsumer<String, String> hook) {
        return lock
            .tryLock(0, TIMEOUT_SECONDS, TimeUnit.SECONDS) // no wait for lock acquisition
            .map(acquired -> {
                if (! acquired) {
                    throw new RequestHandlerLockUnavailable();
                }
                return true;
            })
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "handler lock acquire failed - {} - {}"), lock.getName(), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "handler lock acquire success - {}"), lock.getName()))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, lock.getName(), hook))
            .then()
        ;
    }

    private Mono<Void> acquireLock(ReservationRequest request, RLockReactive lock, BiConsumer<String, String> hook) {
        return lock
            .lock(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "lock acquire failed - {} - {}"), lock.getName(), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock acquire success - {}"), lock.getName()))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, lock.getName(), hook))
        ;
    }

    private Mono<Void> releaseLock(ReservationRequest request, RLockReactive lock, BiConsumer<String, String> hook) {
        return lock
            .forceUnlock()
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "lock release failed - {} - {}"), lock.getName(), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock release success - {}"), lock.getName()))
            .doOnSuccess(ok -> callHook(LOCK_RELEASED, lock.getName(), hook))
            .then()
        ;
    }

    private Mono<ProductAvailability> getProductAvailability(
        ReservationRequest request,
        AtomicReference<ProductAvailability> producAvailabilityRef
    ) {
        return productAvailabilitiesRepo
            .findByProductId(request.getProductId())
            .switchIfEmpty(productAvailabilitiesService.syncWithReservations(request.getProductId()))
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "get product availability failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "get product availability successfully")))
            .doOnSuccess(producAvailabilityRef::set)
        ;
    }

    private Mono<Void> putProductAvailability(ReservationRequest request, ProductAvailability availability, BiConsumer<String, String> hook) {
        return productAvailabilitiesRepo
            .save(availability)
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "put product_availability failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "put product_availability successfully")))
            .doOnSuccess(ok -> callHook(PRODUCT_AVAILABILITY_SAVED, hook))
            .then()
        ;
    }

    private Mono<Void> putReservation(ReservationRequest request, ProductReservation reservation, BiConsumer<String, String> hook) {
        return reservationRepo
            .save(reservation)
            .timeout(Duration.ofSeconds(WAIT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "put product_reservation failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "put product_reservation successfully")))
            .doOnSuccess(ok -> callHook(RESERVATION_SAVED, hook))
            .then()
        ;
    }
}
