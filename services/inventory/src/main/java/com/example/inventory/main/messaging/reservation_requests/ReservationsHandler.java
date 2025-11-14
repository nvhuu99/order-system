package com.example.inventory.main.messaging.reservation_requests;

import com.example.inventory.main.messaging.reservation_requests.exceptions.ProductNotFound;
import com.example.inventory.repositories.product_reservations.entities.ReservationStatus;
import com.example.inventory.main.messaging.reservation_requests.exceptions.InvalidRequestTimestamp;
import com.example.inventory.main.messaging.reservation_requests.exceptions.RequestHandlerLockUnavailable;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.repositories.product_reservations.ProductReservationsCrudRepository;
import com.example.inventory.services.collection_locks.CollectionLocksService;
import com.example.inventory.services.collection_locks.exceptions.LockValueMismatch;
import com.example.inventory.services.collection_locks.exceptions.LocksUnavailable;
import com.example.inventory.services.product_availabilities.ProductAvailabilitiesService;
import com.example.inventory.services.products.ProductsService;
import com.example.inventory.services.products.dto.ProductDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static com.example.inventory.utils.ErrorUtils.exceptionCause;

@Service
public class ReservationsHandler extends ReservationsHandlerProperties {
    
    @Autowired
    private ProductReservationsCrudRepository reservationRepo;

    @Autowired
    private ProductAvailabilitiesRepository productAvailabilitiesRepo;

    @Autowired
    private ProductAvailabilitiesService productAvailabilitiesService;

    @Autowired
    private ProductsService productsSvc;

    @Autowired
    private ReservationValidator validator;

    @Autowired
    private CollectionLocksService locksService;

    public Mono<Void> handle(ReservationRequest request, BiConsumer<String, String> hook) {

        log.info(logTemplate(request, "handling reservation request"));

        var lockValue = UUID.randomUUID().toString();
        var tryAcquireHandlerLock = tryAcquireHandlerLock(request, "order_system:reservation_request_handlers", List.of(request.getIdentifier()), lockValue, hook);
        var releaseHandlerLock = releaseLock(request, "order_system:reservation_request_handlers", List.of(request.getIdentifier()), lockValue, hook);
        var acquireReservationLock = acquireLock(request, "order_system:product_reservations", List.of(request.getProductId()), lockValue, hook);
        var releaseReservationLock = releaseLock(request, "order_system:product_reservations", List.of(request.getProductId()), lockValue, hook);
        var acquireProductAvailabilityLock = acquireLock(request, "order_system:product_availabilities", List.of(request.getProductId()), lockValue, hook);
        var releaseProductAvailabilityLock = releaseLock(request, "order_system:product_availabilities", List.of(request.getProductId()), lockValue, hook);

        var reservationRef = new AtomicReference<ProductReservation>();
        var productAvailabilityRef = new AtomicReference<ProductAvailability>();
        var productRef = new AtomicReference<ProductDetail>();
        var now = Instant.now();

        var commit = Mono.fromRunnable(() -> callHook(REQUEST_COMMITTED, hook));

        return getReservation(request, reservationRef)
            .map(reservation -> {
                validator.checkRequestTimestamp(reservation, request);
                return reservation;
            })
            .then(tryAcquireHandlerLock)
            .then(acquireReservationLock)
            .then(acquireProductAvailabilityLock)
            .then(getProduct(request, productRef))
            .then(getReservation(request, reservationRef))
            .then(getProductAvailability(request, productAvailabilityRef))
            .map(ignored -> {
                validator.checkRequestTimestamp(reservationRef.get(), request);
                return Mono.empty();
            })
            .map(ignored -> {
                var availability = productAvailabilityRef.get();
                var reservation = reservationRef.get();
                var product = productRef.get();

                var reservedTotalAfterExcludeReservation = availability.getReservedAmount() - reservation.getReservedAmount();
                var desiredReserveTotal = reservedTotalAfterExcludeReservation + request.getQuantity();
                var maxAdditional = availability.getStock() < desiredReserveTotal
                    ? availability.getStock() - reservedTotalAfterExcludeReservation
                    : request.getQuantity()
                ;

                availability.setReservedAmount(reservedTotalAfterExcludeReservation + maxAdditional);
                availability.setUpdatedAt(now);

                reservation.setProductId(request.getProductId());
                reservation.setReservedAmount(maxAdditional);
                reservation.setDesiredAmount(request.getQuantity());
                if (request.getQuantity() > maxAdditional) {
                    reservation.setStatus(ReservationStatus.INSUFFICIENT_STOCK.getValue());
                } else {
                    reservation.setStatus(ReservationStatus.OK.getValue());
                }
                reservation.setExpiresAt(now.plusSeconds(product.getReservationsExpireAfterSeconds()));
                reservation.setUpdatedAt(now);

                return Mono.empty();
            })
            .flatMap(ignored ->
                putReservation(request, reservationRef.get(), hook)
                    .then(putProductAvailability(request, productAvailabilityRef.get(), hook))
            )
            .then(releaseProductAvailabilityLock)
            .then(releaseReservationLock)
            .then(releaseHandlerLock)
            .then(commit)
            .onErrorResume(ProductNotFound.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(InvalidRequestTimestamp.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(RequestHandlerLockUnavailable.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(ex ->
                releaseProductAvailabilityLock
                    .then(releaseReservationLock)
                    .then(releaseHandlerLock)
                    .then(Mono.error(ex))
            )
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnSuccess(ok -> log.info(logTemplate(request, "handle reservation request successfully")))
            .then()
        ;
    }

    private Mono<Void> tryAcquireHandlerLock(ReservationRequest request, String collection, List<String> recordIds, String lockValue, BiConsumer<String, String> hook) {
        return locksService
            .tryLock(collection, recordIds, lockValue, Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "try acquire lock failed - {} - {}"), collection, exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "try acquire lock success - {}"), collection))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, collection, hook))
            .onErrorMap(LocksUnavailable.class, ex -> new RequestHandlerLockUnavailable())
            .then()
        ;
    }


    private Mono<Void> acquireLock(ReservationRequest request, String collection, List<String> recordIds, String lockValue, BiConsumer<String, String> hook) {
        return locksService
            .tryLock(collection, recordIds, lockValue, Duration.ofSeconds(TIMEOUT_SECONDS))
            .retryWhen(fixedDelayRetrySpec())
            .doOnError(ex -> log.error(logTemplate(request, "lock acquire failed - {} - {}"), collection, exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock acquire success - {}"), collection))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, collection, hook))
            .then()
        ;
    }

    private Mono<Void> releaseLock(ReservationRequest request, String collection, List<String> recordIds, String lockValue, BiConsumer<String, String> hook) {
        return locksService
            .unlock(collection, recordIds, lockValue)
            .retryWhen(fixedDelayRetrySpec().filter(ex -> !(ex instanceof LockValueMismatch)))
            .doOnError(ex -> log.error(logTemplate(request, "lock release failed - {} - {}"), collection, exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock release success - {}"), collection))
            .doOnSuccess(ok -> callHook(LOCK_RELEASED, collection, hook))
            .then()
        ;
    }

    private Mono<ProductDetail> getProduct(ReservationRequest request, AtomicReference<ProductDetail> productRef) {
        return productsSvc
            .findById(request.getProductId())
            .switchIfEmpty(Mono.error(new ProductNotFound()))
            .doOnError(ex -> log.error(logTemplate(request, "get product failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "get product successfully")))
            .doOnSuccess(productRef::set)
        ;
    }

    private Mono<ProductReservation> getReservation(ReservationRequest request, AtomicReference<ProductReservation> reservationRef) {
        return reservationRepo
            .findByProductIdAndUserId(request.getProductId(), request.getUserId())
            .defaultIfEmpty(
                new ProductReservation(null, request.getUserId(), request.getProductId(), 0, 0, null, null, null)
            )
            .doOnError(ex -> log.error(logTemplate(request, "get reservation failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "get reservation successfully")))
            .doOnSuccess(reservationRef::set)
        ;
    }

    private Mono<ProductAvailability> getProductAvailability(
        ReservationRequest request,
        AtomicReference<ProductAvailability> producAvailabilityRef
    ) {
        return productAvailabilitiesRepo
            .findByProductId(request.getProductId())
            .switchIfEmpty(productAvailabilitiesService.syncWithReservations(request.getProductId()))
            .doOnError(ex -> log.error(logTemplate(request, "get product availability failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "get product availability successfully")))
            .doOnSuccess(producAvailabilityRef::set)
        ;
    }

    private Mono<Void> putProductAvailability(ReservationRequest request, ProductAvailability availability, BiConsumer<String, String> hook) {
        return productAvailabilitiesRepo
            .save(availability)
            .doOnError(ex -> log.error(logTemplate(request, "put product_availability failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "put product_availability successfully")))
            .doOnSuccess(ok -> callHook(PRODUCT_AVAILABILITY_SAVED, hook))
            .then()
        ;
    }

    private Mono<Void> putReservation(ReservationRequest request, ProductReservation reservation, BiConsumer<String, String> hook) {
        return reservationRepo
            .save(reservation)
            .doOnError(ex -> log.error(logTemplate(request, "put product_reservation failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "put product_reservation successfully")))
            .doOnSuccess(ok -> callHook(RESERVATION_SAVED, hook))
            .then()
        ;
    }
}
