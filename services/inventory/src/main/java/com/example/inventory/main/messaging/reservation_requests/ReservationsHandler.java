package com.example.inventory.main.messaging.reservation_requests;

import com.example.inventory.main.messaging.reservation_requests.exceptions.ProductNotFound;
import com.example.inventory.main.messaging.reservation_requests.exceptions.RequestHasAlreadyHandled;
import com.example.inventory.repositories.product_reservations.entities.ReservationStatus;
import com.example.inventory.main.messaging.reservation_requests.exceptions.InvalidRequestTimestamp;
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
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static com.example.inventory.utils.ErrorUtils.exceptionCause;

@Service
public class ReservationsHandler extends ReservationsHandlerProperties {
    
    @Autowired
    private ProductReservationsCrudRepository reservationCrudRepo;

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

    @Autowired
    private ReservationRequestsTracker tracker;

    public Mono<Void> handle(ReservationRequest request, BiConsumer<String, String> hook) {

        log.info(logTemplate(request, "handling reservation request"));

        var lockValue = UUID.randomUUID().toString();

        var tryRequestHandlerLock = tryLock(request, "order_system:reservation_requests", List.of(request.getRequestLockId()), lockValue, hook);
        var waitReservationLock = waitLock(request, "order_system:product_reservations", List.of(request.getReservationLockId()), lockValue, hook);
        var waitProductAvailabilityLock = waitLock(request, "order_system:product_availabilities", List.of(request.getProductAvailabilityLockId()), lockValue, hook);

        var releaseHandlerLock = releaseLock(request, "order_system:reservation_requests", List.of(request.getRequestLockId()), lockValue, hook);
        var releaseReservationLock = releaseLock(request, "order_system:product_reservations", List.of(request.getReservationLockId()), lockValue, hook);
        var releaseProductAvailabilityLock = releaseLock(request, "order_system:product_availabilities", List.of(request.getProductAvailabilityLockId()), lockValue, hook);

        var reservationRef = new AtomicReference<ProductReservation>();
        var productAvailabilityRef = new AtomicReference<ProductAvailability>();
        var productRef = new AtomicReference<ProductDetail>();
        var now = Instant.now();

        var commit = Mono.fromRunnable(() -> callHook(REQUEST_COMMITTED, hook));

        return getReservation(request, reservationRef)
            .then(validateRequest(request, reservationRef))
            .then(tryRequestHandlerLock)
            .then(waitReservationLock)
            .then(waitProductAvailabilityLock)
            .then(skipIfRequestHandledAlready(request))
            .then(Mono.when(
                getProduct(request, productRef),
                getReservation(request, reservationRef),
                getProductAvailability(request, productAvailabilityRef)
            ))
            .then(validateRequest(request, reservationRef))
            .flatMap(ignored -> {
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

                reservation.setProductId(request.getProductId());
                reservation.setReservedAmount(maxAdditional);
                reservation.setDesiredAmount(request.getQuantity());
                if (request.getQuantity() > maxAdditional) {
                    reservation.setStatus(ReservationStatus.INSUFFICIENT_STOCK.getValue());
                } else {
                    reservation.setStatus(ReservationStatus.OK.getValue());
                }
                reservation.setExpiresAt(now.plusSeconds(product.getReservationsExpireAfterSeconds()));
                reservation.setRequestedAt(request.getRequestedAt());

                return Mono.when(
                    putReservation(request, reservationRef.get(), hook),
                    putProductAvailability(request, productAvailabilityRef.get(), hook)
                );
            })
            .then(markRequestAsHandled(request))
            .then(Mono.when(
                releaseProductAvailabilityLock,
                releaseReservationLock
            ))
            .then(releaseHandlerLock)
            .then(commit)
            .onErrorResume(RequestHasAlreadyHandled.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(ProductNotFound.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(InvalidRequestTimestamp.class, ex -> commit.then(Mono.error(ex)))
            .onErrorResume(LocksUnavailable.class, ex -> Objects.equals(ex.getCollectionName(), "order_system:reservation_requests")
                ? commit.then(Mono.error(ex))
                : Mono.error(ex)
            )
            .onErrorResume(ex ->
                Mono.when(
                    releaseProductAvailabilityLock,
                    releaseReservationLock
                )
                .then(releaseHandlerLock)
                .then(Mono.error(ex))
            )
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnSuccess(ok -> log.info(logTemplate(request, "handle reservation request successfully")))
            .then()
        ;
    }


    private Mono<Boolean> skipIfRequestHandledAlready(ReservationRequest request) {
        return tracker
            .hasRequestHandledRecently(request)
            .flatMap(handled -> handled
                ? Mono.error(new RequestHasAlreadyHandled())
                : Mono.just(true)
            )
            .doOnSuccess(ok -> log.debug(logTemplate(request, "recent handled requests check success")))
            .doOnError(ex -> log.error(logTemplate(request, "recent handled requests check failed - {}"), ex.getMessage()))
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }

    private Mono<Boolean> markRequestAsHandled(ReservationRequest request) {
        return tracker
            .addToRecentHandledRequests(request)
            .then(tracker.increaseTotalHandledRequests(request.getProductId()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "tracked recent handled requests")))
            .doOnError(ex -> log.error(logTemplate(request, "track recent handled requests failed - {}"), ex.getMessage()))
            .then(Mono.just(true))
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }

    private Mono<Boolean> validateRequest(ReservationRequest request, AtomicReference<ProductReservation> reservationRef) {
        return Mono
            .fromRunnable(() -> validator.checkRequestTimestamp(reservationRef.get(), request))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "request validation success")))
            .doOnError(ex -> log.error(logTemplate(request, "request validation failed - {}"), ex.getMessage()))
            .then(Mono.just(true))
        ;
    }

    private Mono<Boolean> tryLock(ReservationRequest request, String collection, List<String> recordIds, String lockValue, BiConsumer<String, String> hook) {
        return locksService
            .tryLock(collection, recordIds, lockValue, Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnError(ex -> log.error(logTemplate(request, "try lock failed - {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(lockValueAcquired -> log.debug(logTemplate(request, "try lock success - {}"), collection))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, collection, hook))
            .then(Mono.just(true))
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }

    private Mono<Boolean> waitLock(ReservationRequest request, String collection, List<String> recordIds, String lockValue, BiConsumer<String, String> hook) {
        return locksService
            .tryLock(collection, recordIds, lockValue, Duration.ofSeconds(TIMEOUT_SECONDS))
            .retryWhen(fixedDelayRetrySpec())
            .doOnError(ex -> log.error(logTemplate(request, "wait lock failed - {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(lockValueAcquired -> log.debug(logTemplate(request, "wait lock success - {}"), collection))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, collection, hook))
            .then(Mono.just(true))
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }

    private Mono<Boolean> releaseLock(ReservationRequest request, String collection, List<String> recordIds, String lockValue, BiConsumer<String, String> hook) {
        return locksService
            .unlock(collection, recordIds, lockValue)
            .retryWhen(fixedDelayRetrySpec().filter(ex -> !(ex instanceof LockValueMismatch)))
            .onErrorResume(LockValueMismatch.class, ex -> Mono.empty())
            .doOnError(ex -> log.error(logTemplate(request, "lock release failed - {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock release success - {}"), collection))
            .doOnSuccess(ok -> callHook(LOCK_RELEASED, collection, hook))
            .then(Mono.just(true))
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }

    private Mono<ProductDetail> getProduct(ReservationRequest request, AtomicReference<ProductDetail> productRef) {
        return productsSvc
            .findById(request.getProductId())
            .switchIfEmpty(Mono.error(new ProductNotFound()))
            .doOnError(ex -> log.error(logTemplate(request, "get product failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "get product successfully")))
            .doOnSuccess(productRef::set)
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }

    private Mono<ProductReservation> getReservation(ReservationRequest request, AtomicReference<ProductReservation> reservationRef) {
        return reservationCrudRepo
            .findByProductIdAndUserId(request.getProductId(), request.getUserId())
            .defaultIfEmpty(
                new ProductReservation(null, request.getUserId(), request.getProductId(), 0, 0, null, null, null)
            )
            .doOnError(ex -> log.error(logTemplate(request, "get reservation failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(resv -> log.debug(logTemplate(request, "get reservation successfully")))
            .doOnSuccess(reservationRef::set)
            .subscribeOn(Schedulers.boundedElastic())
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
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }

    private Mono<ProductAvailability> putProductAvailability(ReservationRequest request, ProductAvailability availability, BiConsumer<String, String> hook) {
        return productAvailabilitiesRepo
            .save(availability)
            .doOnError(ex -> log.error(logTemplate(request, "put product_availability failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "put product_availability successfully")))
            .doOnSuccess(ok -> callHook(PRODUCT_AVAILABILITY_SAVED, hook))
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }

    private Mono<ProductReservation> putReservation(ReservationRequest request, ProductReservation reservation, BiConsumer<String, String> hook) {
        return reservationCrudRepo
            .save(reservation)
            .doOnError(ex -> log.error(logTemplate(request, "put product_reservation failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(resv -> log.debug(logTemplate(request, "put product_reservation successfully")))
            .doOnSuccess(ok -> callHook(RESERVATION_SAVED, hook))
            .subscribeOn(Schedulers.boundedElastic())
        ;
    }
}
