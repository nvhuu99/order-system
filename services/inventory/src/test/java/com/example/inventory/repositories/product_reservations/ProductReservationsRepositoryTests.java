package com.example.inventory.repositories.product_reservations;

import com.example.inventory.DatabaseInitializer;
import com.example.inventory.TestBase;
import com.example.inventory.DatabaseSeeder;
import com.example.inventory.enums.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ProductReservationsRepositoryTests extends TestBase {

    @Autowired
    private DatabaseSeeder seeder;

    @Autowired
    private DatabaseInitializer dbInit;

    @Autowired
    private ProductReservationsRepository reservationsRepo;

    @Autowired
    private ProductReservationsCrudRepository reservationsCrudRepo;
    

    @Test
    void sumReservedAmountByProductIds_mustReturnCorrectSumForAllByProductIds_andOnlyIncludeStatusOK_andExcludesExpiredReservation() {

        var p = UUID.randomUUID().toString().substring(1);
        dbInit.createTables().block();
        seeder.seedProduct(p+"1", 2);
        seeder.seedProduct(p+"2", 0);
        seeder.seedProduct(p+"3", 3);
        seeder.seedProduct(p+"4", 4);
        seeder.seedProduct(p+"5", 2);
        seeder.seedProduct(p+"6", 1);
        seeder.seedReservation(p+"1", "u1", 1, 1, ReservationStatus.OK, 0);
        seeder.seedReservation(p+"2", "u1", 0, 2, ReservationStatus.INSUFFICIENT_STOCK, 0);
        seeder.seedReservation(p+"3", "u1", 0, 3, ReservationStatus.EXPIRED, 0);
        seeder.seedReservation(p+"4", "u1", 2, 2, ReservationStatus.OK, 0);
        seeder.seedReservation(p+"4", "u2", 2, 3, ReservationStatus.INSUFFICIENT_STOCK, 0);
        seeder.seedReservation(p+"5", "u1", 1, 1, ReservationStatus.OK, 0);
        seeder.seedReservation(p+"5", "u2", 0, 1, ReservationStatus.EXPIRED, 0);
        seeder.seedReservation(p+"6", "u1", 1, 1, ReservationStatus.OK, 0);

        var ids = List.of(p+"1", p+"2", p+"3", p+"4", p+"5"); // exclude "p6"
        var total = new AtomicInteger();

        reservationsRepo
            .sumReservedAmountByProductIds(ids)
            .doOnNext(data -> {
                total.addAndGet(data.getReserved());
                assertTrue(ids.contains(data.getProductId()));
            })
            .doOnComplete(() -> {
                assertEquals(total.get(), 6);
            })
            .blockLast()
        ;
    }

    @Test
    void syncValidations_mustUpdateReservations_withCorrectStatusAndReservedAmount() {

        var p = UUID.randomUUID().toString().substring(1);
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

        var ids = List.of(p+"1", p+"2", p+"3", p+"4", p+"5");
        reservationsRepo.syncReservations(ids).block();

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


        assertEquals(r11.getReserved(), 0);
        assertEquals(r21.getReserved(), 3);
        assertEquals(r22.getReserved(), 2);
        assertEquals(r23.getReserved(), 0);
        assertEquals(r31.getReserved(), 0);
        assertEquals(r32.getReserved(), 3);
        assertEquals(r41.getReserved(), 2);
        assertEquals(r42.getReserved(), 3);
        assertEquals(r43.getReserved(), 0);
        assertEquals(r51.getReserved(), 2);

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
}
