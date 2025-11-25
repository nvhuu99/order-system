package com.example.inventory.main.workers.reservations_synchronizer;

import com.example.inventory.DatabaseInitializer;
import com.example.inventory.DatabaseSeeder;
import com.example.inventory.TestBase;
import com.example.inventory.repositories.product_reservations.entities.ReservationStatus;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.repositories.product_reservations.ProductReservationsCrudRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

public class SyncRequestsHandlerTests extends TestBase {

    @Autowired
    private DatabaseSeeder seeder;

    @Autowired
    private DatabaseInitializer dbInit;

    @Autowired
    private ProductReservationsCrudRepository reservationsCrudRepo;

    @Autowired
    private ProductAvailabilitiesRepository availabilitiesRepo;

    @Autowired
    private SyncRequestsHandler handler;

    private void putHookToMap(ConcurrentHashMap<String, List<String>> map, String hook, String value) {
        var values = map.getOrDefault(hook, new ArrayList<>());
        values.add(value);
        map.put(hook, values);
    }

    private void setupData(String p) {
        dbInit.createTables().block();
        seeder.seedProduct(p+"1", 0);
        seeder.seedProduct(p+"2", 5);
        seeder.seedProduct(p+"3", 3);
        seeder.seedProduct(p+"4", 10);
        seeder.seedProduct(p+"5", 2);
        seeder.seedReservation(p+"1", "u1", 0, 1, ReservationStatus.UNKOWN, 0);
        seeder.seedReservation(p+"2", "u1", 0, 3, ReservationStatus.UNKOWN, 0);
        seeder.seedReservation(p+"2", "u2", 0, 3, ReservationStatus.UNKOWN, 1);
        seeder.seedReservation(p+"2", "u3", 0, 1, ReservationStatus.UNKOWN, 2);
        seeder.seedReservation(p+"3", "u1", 0, 2, ReservationStatus.EXPIRED, 0);
        seeder.seedReservation(p+"3", "u2", 0, 3, ReservationStatus.UNKOWN, 1);
        seeder.seedReservation(p+"4", "u1", 0, 2, ReservationStatus.UNKOWN, 0);
        seeder.seedReservation(p+"4", "u2", 0, 3, ReservationStatus.UNKOWN, 1);
        seeder.seedReservation(p+"4", "u3", 0, 5, ReservationStatus.EXPIRED, 2);
        seeder.seedReservation(p+"5", "u1", 0, 3, ReservationStatus.UNKOWN, 0);
    }

    @Test
    void hooksAreCalled() {
        var hooks = new ConcurrentHashMap<String, List<String>>();
        var p = UUID.randomUUID().toString().substring(1);
        setupData(p);

        var request = new SyncRequest(UUID.randomUUID().toString(), 5, 1, Instant.now().plusSeconds(300));
        handler.handle(request, (hook, value) -> putHookToMap(hooks, hook, value)).block();

        assertTrue(hooks.containsKey("LOCK_ACQUIRED"));
        assertTrue(hooks.containsKey("LOCK_RELEASED"));
        assertTrue(hooks.containsKey("ZERO_AMOUNT_RESERVATIONS_REMOVED"));
        assertTrue(hooks.containsKey("RESERVATIONS_SYNCED"));
        assertTrue(hooks.containsKey("PRODUCT_AVAILABILITY_SYNCED"));
        assertTrue(hooks.containsKey("REQUEST_COMMITTED"));

        assertEquals(hooks.get("LOCK_ACQUIRED").get(0), "order_system:product_reservations");
        assertEquals(hooks.get("LOCK_ACQUIRED").get(1), "order_system:product_availabilities");
        assertEquals(hooks.get("LOCK_RELEASED").get(0), "order_system:product_reservations");
        assertEquals(hooks.get("LOCK_RELEASED").get(1), "order_system:product_availabilities");
    }

    @Test
    void mustUpdateReservations_withCorrectStatusAndReservedAmount() {
        var p = UUID.randomUUID().toString().substring(1);
        setupData(p);

        var request = new SyncRequest(UUID.randomUUID().toString(),5, 1, Instant.now().plusSeconds(300));
        handler.handle(request, null).block();

        var r11 = reservationsCrudRepo.findByProductIdAndUserId(p+"1", "u1").block();
        var r21 = reservationsCrudRepo.findByProductIdAndUserId(p+"2", "u1").block();
        var r22 = reservationsCrudRepo.findByProductIdAndUserId(p+"2", "u2").block();
        var r23 = reservationsCrudRepo.findByProductIdAndUserId(p+"2", "u3").block();
        var r31 = reservationsCrudRepo.findByProductIdAndUserId(p+"3", "u1").block();
        var r32 = reservationsCrudRepo.findByProductIdAndUserId(p+"3", "u2").block();
        var r41 = reservationsCrudRepo.findByProductIdAndUserId(p+"4", "u1").block();
        var r42 = reservationsCrudRepo.findByProductIdAndUserId(p+"4", "u2").block();
        var r43 = reservationsCrudRepo.findByProductIdAndUserId(p+"4", "u3").block();
        var r51 = reservationsCrudRepo.findByProductIdAndUserId(p+"5", "u1").block();

        assertNotNull(r11);
        assertNotNull(r21);
        assertNotNull(r22);
        assertNotNull(r23);
        assertNotNull(r31);
        assertNotNull(r32);
        assertNotNull(r41);
        assertNotNull(r42);
        assertNotNull(r43);
        assertNotNull(r51);

        assertEquals(r11.getReservedAmount(), 0);
        assertEquals(r21.getReservedAmount(), 3);
        assertEquals(r22.getReservedAmount(), 2);
        assertEquals(r23.getReservedAmount(), 0);
        assertEquals(r31.getReservedAmount(), 0);
        assertEquals(r32.getReservedAmount(), 3);
        assertEquals(r41.getReservedAmount(), 2);
        assertEquals(r42.getReservedAmount(), 3);
        assertEquals(r43.getReservedAmount(), 0);
        assertEquals(r51.getReservedAmount(), 2);

        assertEquals(r11.getStatus(), ReservationStatus.INSUFFICIENT_STOCK.getValue());
        assertEquals(r21.getStatus(), ReservationStatus.OK.getValue());
        assertEquals(r22.getStatus(), ReservationStatus.INSUFFICIENT_STOCK.getValue());
        assertEquals(r23.getStatus(), ReservationStatus.INSUFFICIENT_STOCK.getValue());
        assertEquals(r31.getStatus(), ReservationStatus.EXPIRED.getValue());
        assertEquals(r32.getStatus(), ReservationStatus.OK.getValue());
        assertEquals(r41.getStatus(), ReservationStatus.OK.getValue());
        assertEquals(r42.getStatus(), ReservationStatus.OK.getValue());
        assertEquals(r43.getStatus(), ReservationStatus.EXPIRED.getValue());
        assertEquals(r51.getStatus(), ReservationStatus.INSUFFICIENT_STOCK.getValue());
    }

    @Test
    void mustUpdateProductAvailabilities_withCorrectReservedAmount() {
        var p = UUID.randomUUID().toString().substring(1);
        setupData(p);

        var request = new SyncRequest(UUID.randomUUID().toString(),5, 1, Instant.now().plusSeconds(300));
        handler.handle(request, null).block();

        var a1 = availabilitiesRepo.findByProductId(p+"1").block();
        var a2 = availabilitiesRepo.findByProductId(p+"2").block();
        var a3 = availabilitiesRepo.findByProductId(p+"3").block();
        var a4 = availabilitiesRepo.findByProductId(p+"4").block();
        var a5 = availabilitiesRepo.findByProductId(p+"5").block();

        assertNotNull(a1);
        assertNotNull(a2);
        assertNotNull(a3);
        assertNotNull(a4);
        assertNotNull(a5);

        assertEquals(a1.getReservedAmount(), 0);
        assertEquals(a2.getReservedAmount(), 5);
        assertEquals(a3.getReservedAmount(), 3);
        assertEquals(a4.getReservedAmount(), 5);
        assertEquals(a5.getReservedAmount(), 2);
    }

    @Test
    void mustRemoveZeroAmountReservations() {
        var p = UUID.randomUUID().toString().substring(1);
        dbInit.createTables().block();
        seeder.seedProduct(p+"1", 5);
        seeder.seedProduct(p+"2", 5);
        seeder.seedReservation(p+"1", "u1", 1, 1, ReservationStatus.OK, 0);
        seeder.seedReservation(p+"1", "u2", 0, 0, ReservationStatus.OK, 0);
        seeder.seedReservation(p+"2", "u1", 1, 1, ReservationStatus.OK, 0);
        seeder.seedReservation(p+"2", "u2", 0, 0, ReservationStatus.OK, 0);

        var request = new SyncRequest(UUID.randomUUID().toString(),5, 1, Instant.now().plusSeconds(300));
        handler.handle(request, null).block();

        var r11 = reservationsCrudRepo.findByProductIdAndUserId(p+"1", "u1").block();
        var r12 = reservationsCrudRepo.findByProductIdAndUserId(p+"1", "u2").block();
        var r21 = reservationsCrudRepo.findByProductIdAndUserId(p+"2", "u1").block();
        var r22 = reservationsCrudRepo.findByProductIdAndUserId(p+"2", "u2").block();

        assertNull(r12);
        assertNull(r22);

        assertNotNull(r11);
        assertNotNull(r21);
    }
}
