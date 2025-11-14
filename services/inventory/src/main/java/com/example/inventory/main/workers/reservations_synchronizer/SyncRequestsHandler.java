package com.example.inventory.main.workers.reservations_synchronizer;

import com.example.inventory.repositories.product_reservations.ProductReservationsRepository;
import com.example.inventory.repositories.products.ProductsRepository;
import com.example.inventory.repositories.products.dto.ListRequest;
import com.example.inventory.repositories.products.entities.Product;
import com.example.inventory.services.collection_locks.CollectionLocksService;
import com.example.inventory.services.collection_locks.exceptions.LockValueMismatch;
import com.example.inventory.services.product_availabilities.ProductAvailabilitiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import static com.example.inventory.utils.ErrorUtils.exceptionCause;

@Service
public class SyncRequestsHandler extends SyncRequestsHandlerProperties {

    @Autowired
    private ProductsRepository productsRepo;

    @Autowired
    private ProductReservationsRepository reservationRepo;

    @Autowired
    private ProductAvailabilitiesService productAvailabilitiesService;

    @Autowired
    private CollectionLocksService locksService;


    public Mono<Void> handle(SyncRequest request, BiConsumer<String, String> hook) {

        log.info(logTemplate(request, "handling sync request"));

        var lockVal = UUID.randomUUID().toString();
        var commit = Mono.fromRunnable(() -> callHook(REQUEST_COMMITTED, hook));

        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(Instant.now())) {
            log.info(logTemplate(request, "sync request expired"));
            return commit.then();
        }

        var productIds = getProductIds(request).block();
        if (productIds != null && productIds.isEmpty()) {
            return commit.then();
        }

        return acquireLock(request, "order_system:product_reservations", productIds, lockVal, hook)
            .then(acquireLock(request, "order_system:product_availabilities", productIds, lockVal, hook))
            .then(removeZeroAmountReservations(request, productIds, hook))
            .then(syncReservations(request, productIds, hook))
            .then(syncProductAvailability(request, productIds, hook))
            .then(releaseLock(request, "order_system:product_reservations", productIds, lockVal, hook))
            .then(releaseLock(request, "order_system:product_availabilities", productIds, lockVal, hook))
            .then(commit)
            .onErrorResume(ex ->
                releaseLock(request, "order_system:product_reservations", productIds, lockVal, hook)
                    .then(releaseLock(request, "order_system:product_availabilities", productIds, lockVal, hook))
                    .then(Mono.error(ex))
            )
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .doOnSuccess(ok -> log.info(logTemplate(request, "handle sync request successfully")))
            .then()
        ;
    }


    private Mono<List<String>> getProductIds(SyncRequest request) {
        var args = new ListRequest();
        args.setPage(request.getBatchNumber());
        args.setLimit(request.getBatchSize());
        return productsRepo
            .list(args)
            .map(Product::getId)
            .collectList()
            .defaultIfEmpty(new ArrayList<>())
            .doOnError(ex -> log.error(logTemplate(request, "get product_ids failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "get product_ids successfully")))
        ;
    }

    private Mono<Void> acquireLock(SyncRequest request, String collection, List<String> recordIds, String lockValue, BiConsumer<String, String> hook) {
        return locksService
            .tryLock(collection, recordIds, lockValue, Duration.ofSeconds(TIMEOUT_SECONDS))
            .retryWhen(fixedDelayRetrySpec())
            .doOnError(ex -> log.error(logTemplate(request, "lock acquire failed - {} - {}"), collection, exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock acquire success - {}"), collection))
            .doOnSuccess(ok -> callHook(LOCK_ACQUIRED, collection, hook))
            .then()
        ;
    }

    private Mono<Void> releaseLock(SyncRequest request, String collection, List<String> recordIds, String lockValue, BiConsumer<String, String> hook) {
        return locksService
            .unlock(collection, recordIds, lockValue)
            .retryWhen(fixedDelayRetrySpec().filter(ex -> !(ex instanceof LockValueMismatch)))
            .doOnError(ex -> log.error(logTemplate(request, "lock release failed - {} - {}"), collection, exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "lock release success - {}"), collection))
            .doOnSuccess(ok -> callHook(LOCK_RELEASED, collection, hook))
            .then()
        ;
    }

    private Mono<Void> removeZeroAmountReservations(SyncRequest request, List<String> productIds, BiConsumer<String, String> hook) {
        return reservationRepo
            .removeZeroAmountReservations(productIds)
            .doOnError(ex -> log.error(logTemplate(request, "remove product_reservations with desired_amount equals zero failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "remove product_reservations with desired_amount equals zero successfully")))
            .doOnSuccess(ok -> callHook(ZERO_AMOUNT_RESERVATIONS_REMOVED, hook))
            .then()
        ;
    }

    private Mono<Void> syncProductAvailability(SyncRequest request, List<String> productIds, BiConsumer<String, String> hook) {
        return productAvailabilitiesService
            .syncAllWithReservations(productIds)
            .doOnError(ex -> log.error(logTemplate(request, "sync product_availability failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "sync product_availability successfully")))
            .doOnSuccess(ok -> callHook(PRODUCT_AVAILABILITY_SYNCED, hook))
            .then()
        ;
    }

    private Mono<Void> syncReservations(SyncRequest request, List<String> productIds, BiConsumer<String, String> hook) {
        return reservationRepo
            .syncReservations(productIds)
            .doOnError(ex -> log.error(logTemplate(request, "sync product_reservations failed: {}"), exceptionCause(ex).getMessage()))
            .doOnSuccess(ok -> log.debug(logTemplate(request, "sync product_reservations successfully")))
            .doOnSuccess(ok -> callHook(RESERVATIONS_SYNCED, hook))
            .then()
        ;
    }
}
