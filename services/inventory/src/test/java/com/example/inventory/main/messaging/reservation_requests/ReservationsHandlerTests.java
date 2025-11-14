package com.example.inventory.main.messaging.reservation_requests;

import com.example.inventory.DatabaseInitializer;
import com.example.inventory.TestBase;
import com.example.inventory.repositories.product_reservations.entities.ReservationStatus;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.repositories.product_reservations.ProductReservationsCrudRepository;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import com.example.inventory.repositories.products.ProductsCrudRepository;
import com.example.inventory.repositories.products.entities.Product;
import com.example.inventory.main.messaging.reservation_requests.exceptions.InvalidRequestTimestamp;
import com.example.inventory.services.collection_locks.CollectionLocksService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.inventory.main.messaging.reservation_requests.ReservationsHandlerProperties.RESERVATION_SAVED;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationsHandlerTests extends TestBase {

    @Autowired
    private DatabaseInitializer initializer;

    @Autowired
    private ReservationsHandler handler;

    @Autowired
    private ProductReservationsCrudRepository reservationsRepo;

    @Autowired
    private ProductsCrudRepository productsRepo;

    @Autowired
    private ProductAvailabilitiesRepository availabilitiesRepo;

    @Autowired
    private CollectionLocksService locksService;


    private ProductReservation setupReservation(Integer stock, Integer reserved, Integer desired, ReservationStatus status) {
        var now = Instant.now();
        var prodName = UUID.randomUUID().toString();
        var userId = UUID.randomUUID().toString();
        var product = new Product(null, prodName, BigDecimal.valueOf(1 + 10 * Math.random()), stock, 300, now);
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

        var acquireHandlerLock = locksService
            .tryLock("order_system:reservation_request_handlers", List.of(request.getIdentifier()), "lockValue", Duration.ofSeconds(10))
        ;
        acquireHandlerLock
            .then(handler.handle(request, (hook, value) -> putHookToMap(hooks, hook, value)))
            .onErrorComplete()
            .block()
        ;

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
        assertEquals(hooks.get("LOCK_ACQUIRED").get(0), "order_system:reservation_request_handlers");
        assertEquals(hooks.get("LOCK_ACQUIRED").get(1), "order_system:product_reservations");
        assertEquals(hooks.get("LOCK_ACQUIRED").get(2), "order_system:product_availabilities");
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
        assertEquals(hooks.get("LOCK_RELEASED").get(0), "order_system:product_availabilities");
        assertEquals(hooks.get("LOCK_RELEASED").get(1), "order_system:product_reservations");
        assertEquals(hooks.get("LOCK_RELEASED").get(2), "order_system:reservation_request_handlers");
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
        assertEquals(reservationResult.getReservedAmount(), request.getQuantity());
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
        assertEquals(reservationResult.getReservedAmount(), request.getQuantity());
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
        assertEquals(secondReservation.getReservedAmount(), 3);
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
        assertEquals(secondReservation.getReservedAmount(), request.getQuantity());
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
        assertEquals(availability.getReservedAmount(), reservation.getReservedAmount() + request.getQuantity());
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
        assertEquals(availability.getReservedAmount(), request.getQuantity());
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
        assertEquals(availability.getReservedAmount(), 4);
        assertNotNull(availability.getUpdatedAt());
    }
}
