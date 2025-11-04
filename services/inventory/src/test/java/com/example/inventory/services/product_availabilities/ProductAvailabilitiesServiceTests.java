package com.example.inventory.services.product_availabilities;

import com.example.inventory.DatabaseInitializer;
import com.example.inventory.DatabaseSeeder;
import com.example.inventory.TestBase;
import com.example.inventory.enums.ReservationStatus;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ProductAvailabilitiesServiceTests extends TestBase {

    @Autowired
    private DatabaseSeeder seeder;

    @Autowired
    private DatabaseInitializer dbInit;

    @Autowired
    private ProductAvailabilitiesRepository productAvailabilitiesRepo;

    @Autowired
    private ProductAvailabilitiesService productAvailabilitiesService;

    private void setup(String productIdPrefix) {
        var p = productIdPrefix;
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
        productAvailabilitiesService
            .syncAllWithReservations(List.of(p+"1", p+"2", p+"3", p+"4", p+"5", p+"6"))
            .block()
        ;
    }


    @Test
    void syncWithReservations_afterSync_dataShouldMatch() {
        var p = UUID.randomUUID().toString().substring(1);
        setup(p);

        productAvailabilitiesService.syncWithReservations(p+"2")
            .then(productAvailabilitiesService.syncWithReservations(p+"3"))
            .then(productAvailabilitiesService.syncWithReservations(p+"4"))
            .block()
        ;

        var a2 = productAvailabilitiesRepo.findByProductId(p+"2").block();
        var a3 = productAvailabilitiesRepo.findByProductId(p+"3").block();
        var a4 = productAvailabilitiesRepo.findByProductId(p+"4").block();

        assertEquals(a2.getReserved(), 0);
        assertEquals(a3.getReserved(), 0);
        assertEquals(a4.getReserved(), 4);
    }

    @Test
    void syncAllWithReservations_afterSync_dataShouldMatch() {
        var p = UUID.randomUUID().toString().substring(1);
        setup(p);

        var a1 = productAvailabilitiesRepo.findByProductId(p+"1").block();
        var a2 = productAvailabilitiesRepo.findByProductId(p+"2").block();
        var a3 = productAvailabilitiesRepo.findByProductId(p+"3").block();
        var a4 = productAvailabilitiesRepo.findByProductId(p+"4").block();
        var a5 = productAvailabilitiesRepo.findByProductId(p+"5").block();
        var a6 = productAvailabilitiesRepo.findByProductId(p+"6").block();

        assertEquals(a1.getReserved(), 1);
        assertEquals(a2.getReserved(), 0);
        assertEquals(a3.getReserved(), 0);
        assertEquals(a4.getReserved(), 4);
        assertEquals(a5.getReserved(), 1);
        assertEquals(a6.getReserved(), 1);
    }
}
