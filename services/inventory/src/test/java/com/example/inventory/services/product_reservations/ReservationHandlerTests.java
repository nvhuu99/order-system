package com.example.inventory.services.product_reservations;

import com.example.inventory.DatabaseInitializer;
import com.example.inventory.TestBase;
import com.example.inventory.enums.ReservationStatus;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.repositories.product_reservations.ProductReservationsCrudRepository;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.repositories.products.ProductsRepository;
import com.example.inventory.repositories.products.entities.Product;
import com.example.inventory.services.product_reservations.exceptions.InvalidRequestTimestamp;
import com.example.inventory.services.product_reservations.exceptions.RequestHandlerLockUnavailable;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.example.inventory.services.product_reservations.ReservationHandlerProperties.RESERVATION_SAVED;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationHandlerTests extends TestBase {

    @Autowired
    private DatabaseInitializer initializer;

    @Autowired
    private ReservationHandler handler;

    @Autowired
    private ProductReservationsCrudRepository reservationsRepo;

    @Autowired
    private ProductsRepository productsRepo;

    @Autowired
    private ProductAvailabilitiesRepository availabilitiesRepo;

    @Autowired
    private RedissonReactiveClient redisson;

    private ProductReservation setupReservation(Integer stock, Integer reserved, Integer desired, ReservationStatus status) {
        var now = Instant.now();
        var prodName = UUID.randomUUID().toString();
        var userId = UUID.randomUUID().toString();
        var product = new Product(null, prodName, BigDecimal.valueOf(1 + 10 * Math.random()), stock, now);
        var expiresAt = status == ReservationStatus.EXPIRED ? now.minus(1, ChronoUnit.HOURS) : now.plus(1, ChronoUnit.HOURS);
        var reservation = new ProductReservation(null, userId, null, reserved, desired, status.getValue(), expiresAt, now);
        return initializer
            .createTables()
            .then(productsRepo.save(product))
            .flatMap(savedProduct -> {
                reservation.setProductId(savedProduct.getId());
                return reservationsRepo.save(reservation);
            })
            .block()
        ;
    }

    private void putHookToMap(ConcurrentHashMap<String, List<String>> map, String hook, String value) {
        var values = map.getOrDefault(hook, new ArrayList<>());
        values.add(value);
        map.put(hook, values);
    }


    @Test
    void ifRequestHasInvalidTimestamp_failFastWithRequestCommitted_andDoNotAcquireHandlerLock() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var reservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(reservation.getProductId(), reservation.getUserId(), 1, Instant.now().minusSeconds(1000));

        var execute = handler
            .handle(request, (hook, value) -> putHookToMap(hooks, hook, value))
        ;

        assertThrows(InvalidRequestTimestamp.class, execute::block);
        assertTrue(hooks.containsKey("REQUEST_COMMITTED"));
        assertFalse(hooks.containsKey("LOCK_ACQUIRED"));
        assertFalse(hooks.containsKey("RESERVATION_SAVED"));
        assertFalse(hooks.containsKey("PRODUCT_AVAILABILITY_SAVED"));
    }

    @Test
    void ifFailedToAcquireHandlerLock_failFastWithRequestCommitted() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var firstReservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(firstReservation.getProductId(), UUID.randomUUID().toString(), 1, Instant.now().plusSeconds(10));

        var handlerLock = redisson
            .getReadWriteLock("order_system:reservation_request_handlers:" + request.getIdentifier())
            .writeLock()
        ;
        var execute = handlerLock
            .tryLock(0, 10, TimeUnit.SECONDS)
            .then(handler.handle(request, (hook, value) -> putHookToMap(hooks, hook, value)))
        ;

        assertThrows(RequestHandlerLockUnavailable.class, execute::block);
        assertTrue(hooks.containsKey("REQUEST_COMMITTED"));
        assertFalse(hooks.containsKey("LOCK_ACQUIRED"));
        assertFalse(hooks.containsKey("RESERVATION_SAVED"));
        assertFalse(hooks.containsKey("PRODUCT_AVAILABILITY_SAVED"));
    }

    @Test
    void ifRequestTimestampIsValid_allLockMustBeAcquired() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var firstReservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(firstReservation.getProductId(), UUID.randomUUID().toString(), 1, Instant.now().plusSeconds(10));

        handler.handle(request, (hook, value) -> putHookToMap(hooks, hook, value)).block();

        assertTrue(hooks.containsKey("REQUEST_COMMITTED"));
        assertTrue(hooks.containsKey("LOCK_ACQUIRED"));
        assertEquals(hooks.get("LOCK_ACQUIRED").get(0), "order_system:reservation_request_handlers:" + request.getIdentifier());
        assertEquals(hooks.get("LOCK_ACQUIRED").get(1), "order_system:product_reservations:" + request.getIdentifier());
        assertEquals(hooks.get("LOCK_ACQUIRED").get(2), "order_system:product_availabilities:" + request.getProductId());
    }

    @Test
    void whenUnhandledErrorOccurred_mustNotCommitRequest_andAllLocksAreReleased() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var firstReservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(firstReservation.getProductId(), UUID.randomUUID().toString(), 1, Instant.now().plusSeconds(10));

        handler
            .handle(request, (hook, value) -> {
                var values = hooks.getOrDefault(hook, new ArrayList<>());
                values.add(value);
                hooks.put(hook, values);
                if (hook.equals(RESERVATION_SAVED)) {
                    throw new RuntimeException();
                }
            })
            .onErrorComplete()
            .block()
        ;

        assertFalse(hooks.containsKey("REQUEST_COMMITTED"));
        assertTrue(hooks.containsKey("LOCK_ACQUIRED"));
        assertEquals(hooks.get("LOCK_RELEASED").get(0), "order_system:product_availabilities:" + request.getProductId());
        assertEquals(hooks.get("LOCK_RELEASED").get(1), "order_system:product_reservations:" + request.getIdentifier());
        assertEquals(hooks.get("LOCK_RELEASED").get(2), "order_system:reservation_request_handlers:" + request.getIdentifier());
    }

    @Test
    void ifReservationNotExisted_createNewReservation() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var firstReservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(firstReservation.getProductId(), UUID.randomUUID().toString(), 1, Instant.now().plusSeconds(10));

        var reservationResult = handler
            .handle(request, (hook, value) -> putHookToMap(hooks, hook, value))
            .then(reservationsRepo.findByProductIdAndUserId(request.getProductId(), request.getUserId()))
            .block()
        ;

        assertTrue(hooks.containsKey("LOCK_ACQUIRED"));
        assertTrue(hooks.containsKey("LOCK_RELEASED"));
        assertTrue(hooks.containsKey("RESERVATION_SAVED"));
        assertTrue(hooks.containsKey("PRODUCT_AVAILABILITY_SAVED"));
        assertTrue(hooks.containsKey("REQUEST_COMMITTED"));

        assertNotNull(reservationResult);
        assertEquals(reservationResult.getReserved(), request.getQuantity());
        assertEquals(reservationResult.getDesiredAmount(), request.getQuantity());
        assertNotNull(reservationResult.getUpdatedAt());
        assertEquals(reservationResult.getStatus(), ReservationStatus.OK.getValue());
    }

    @Test
    void ifReservationAlreadyExisted_putReservation() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var reservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(reservation.getProductId(), reservation.getUserId(), 1, Instant.now().plusSeconds(10));

        var reservationResult = handler
            .handle(request, (hook, value) -> putHookToMap(hooks, hook, value))
            .then(reservationsRepo.findByProductIdAndUserId(request.getProductId(), request.getUserId()))
            .block()
        ;

        assertTrue(hooks.containsKey("LOCK_ACQUIRED"));
        assertTrue(hooks.containsKey("LOCK_RELEASED"));
        assertTrue(hooks.containsKey("RESERVATION_SAVED"));
        assertTrue(hooks.containsKey("PRODUCT_AVAILABILITY_SAVED"));
        assertTrue(hooks.containsKey("REQUEST_COMMITTED"));

        assertNotNull(reservationResult);
        assertEquals(reservationResult.getReserved(), request.getQuantity());
        assertEquals(reservationResult.getDesiredAmount(), request.getQuantity());
        assertNotNull(reservationResult.getUpdatedAt());
        assertEquals(reservationResult.getStatus(), ReservationStatus.OK.getValue());
    }

    @Test
    void ifInSufficientStockForReservation_putReservationWithCorrectStatus() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var firstReservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(firstReservation.getProductId(), UUID.randomUUID().toString(), 5, Instant.now().plusSeconds(10));

        var secondReservation = handler
            .handle(request, (hook, value) -> putHookToMap(hooks, hook, value))
            .then(reservationsRepo.findByProductIdAndUserId(request.getProductId(), request.getUserId()))
            .block()
        ;

        assertNotNull(secondReservation);
        assertEquals(secondReservation.getReserved(), 3);
        assertEquals(secondReservation.getDesiredAmount(), 5);
        assertNotNull(secondReservation.getUpdatedAt());
        assertEquals(secondReservation.getStatus(), ReservationStatus.INSUFFICIENT_STOCK.getValue());
    }

    @Test
    void ifStockSufficientForReservation_putReservationWithCorrectStatus() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var firstReservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(firstReservation.getProductId(), UUID.randomUUID().toString(), 2, Instant.now().plusSeconds(10));

        var secondReservation = handler
            .handle(request, (hook, value) -> putHookToMap(hooks, hook, value))
            .then(reservationsRepo.findByProductIdAndUserId(request.getProductId(), request.getUserId()))
            .block()
        ;

        assertNotNull(secondReservation);
        assertEquals(secondReservation.getReserved(), request.getQuantity());
        assertEquals(secondReservation.getDesiredAmount(), request.getQuantity());
        assertNotNull(secondReservation.getUpdatedAt());
        assertEquals(secondReservation.getStatus(), ReservationStatus.OK.getValue());
    }

    @Test
    void ifNewReservation_andStockSufficient_updateProductAvailabilityAddByQuantity() {
        var reservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(reservation.getProductId(), UUID.randomUUID().toString(), 1, Instant.now().plusSeconds(10));
        var availability = handler
            .handle(request, null)
            .then(availabilitiesRepo.findByProductId(reservation.getProductId()))
            .block()
        ;

        assertNotNull(availability);
        assertEquals(availability.getStock(), 4);
        assertEquals(availability.getReserved(), reservation.getReserved() + request.getQuantity());
        assertNotNull(availability.getUpdatedAt());
    }

    @Test
    void ifExistingReservation_andStockSufficient_updateProductAvailabilityReplaceByQuantity() {
        var reservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(reservation.getProductId(), reservation.getUserId(), 2, Instant.now().plusSeconds(10));
        var availability = handler
            .handle(request, null)
            .then(availabilitiesRepo.findByProductId(reservation.getProductId()))
            .block()
        ;

        assertNotNull(availability);
        assertEquals(availability.getStock(), 4);
        assertEquals(availability.getReserved(), request.getQuantity());
        assertNotNull(availability.getUpdatedAt());
    }

    @Test
    void ifExistingReservation_andInSufficientStock_updateProductAvailabilityAddMaxAvailableQuantity() {
        var reservation = setupReservation(4, 1, 1, ReservationStatus.OK);
        var request = new ReservationRequest(reservation.getProductId(), reservation.getUserId(), 7, Instant.now().plusSeconds(10));
        var availability = handler
            .handle(request, null)
            .then(availabilitiesRepo.findByProductId(request.getProductId()))
            .block()
        ;

        assertNotNull(availability);
        assertEquals(availability.getStock(), 4);
        assertEquals(availability.getReserved(), 4);
        assertNotNull(availability.getUpdatedAt());
    }
}
