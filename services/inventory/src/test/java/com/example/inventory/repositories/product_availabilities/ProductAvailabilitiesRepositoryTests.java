package com.example.inventory.repositories.product_availabilities;

import com.example.inventory.DatabaseInitializer;
import com.example.inventory.DatabaseSeeder;
import com.example.inventory.TestBase;
import com.example.inventory.repositories.product_reservations.entities.ReservationStatus;
import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import com.example.inventory.services.product_availabilities.ProductAvailabilitiesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ProductAvailabilitiesRepositoryTests extends TestBase {

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
    public void findByProductId_mustReturnData_whenExists() {
        var p = UUID.randomUUID().toString().substring(1);
        setup(p);

        var now = Instant.now();
        var a1 = productAvailabilitiesRepo.findByProductId(p+"1").block();
        var a4 = productAvailabilitiesRepo.findByProductId(p+"4").block();

        assertNotNull(a1);
        assertNotNull(a4);

        assertEquals(a1.getProductId(), p+"1");
        assertEquals(a1.getReservedAmount(), 1);
        assertEquals(a1.getStock(), 2);
        assertTrue(a1.getUpdatedAt().isBefore(now));

        assertEquals(a4.getProductId(), p+"4");
        assertEquals(a4.getReservedAmount(), 4);
        assertEquals(a4.getStock(), 4);
        assertTrue(a4.getUpdatedAt().isBefore(now));
    }

    @Test
    public void findAllByProductIds_mustReturnData_whenExists() {
        var p = UUID.randomUUID().toString().substring(1);
        setup(p);

        var now = Instant.now();
        var results = productAvailabilitiesRepo.findAllByProductIds(List.of(p+"3", p+5)).block();

        assertEquals(results.size(), 2);

        var a3 = results.get(0);
        var a5 = results.get(1);

        assertNotNull(a3);
        assertNotNull(a5);

        assertEquals(a3.getProductId(), p+"3");
        assertEquals(a3.getReservedAmount(), 0);
        assertEquals(a3.getStock(), 3);
        assertTrue(a3.getUpdatedAt().isBefore(now));

        assertEquals(a5.getProductId(), p+"5");
        assertEquals(a5.getReservedAmount(), 1);
        assertEquals(a5.getStock(), 2);
        assertTrue(a5.getUpdatedAt().isBefore(now));
    }

    @Test
    public void save_mustSuccess() {
        var p = UUID.randomUUID().toString().substring(1);
        setup(p);

        var data = new ProductAvailability();
        data.setProductId(UUID.randomUUID().toString());
        data.setStock(4);
        data.setReservedAmount(2);
        data.setUpdatedAt(Instant.now());
        productAvailabilitiesRepo.save(data).block();

        var now = Instant.now();
        var findResult = productAvailabilitiesRepo.findByProductId(data.getProductId()).block();

        assertNotNull(findResult);
        assertEquals(data.getProductId(), findResult.getProductId());
        assertEquals(data.getStock(), findResult.getStock());
        assertEquals(data.getReservedAmount(), findResult.getReservedAmount());
        assertTrue(findResult.getUpdatedAt().isBefore(now));
    }

    @Test
    public void saveMany_mustSuccess() {
        var p = UUID.randomUUID().toString().substring(1);
        setup(p);

        var r1 = new ProductAvailability();
        var r2 = new ProductAvailability();
        r1.setProductId(UUID.randomUUID().toString());
        r1.setStock(4);
        r1.setReservedAmount(2);
        r1.setUpdatedAt(Instant.now());
        r2.setProductId(UUID.randomUUID().toString());
        r2.setStock(6);
        r2.setReservedAmount(1);
        r2.setUpdatedAt(Instant.now());
        productAvailabilitiesRepo.saveMany(List.of(r1, r2)).block();

        var now = Instant.now();
        var f1 = productAvailabilitiesRepo.findByProductId(r1.getProductId()).block();
        var f2 = productAvailabilitiesRepo.findByProductId(r2.getProductId()).block();

        assertNotNull(f1);
        assertEquals(r1.getProductId(), f1.getProductId());
        assertEquals(r1.getStock(), f1.getStock());
        assertEquals(r1.getReservedAmount(), f1.getReservedAmount());
        assertTrue(r1.getUpdatedAt().isBefore(now));

        assertNotNull(f2);
        assertEquals(r2.getProductId(), f2.getProductId());
        assertEquals(r2.getStock(), f2.getStock());
        assertEquals(r2.getReservedAmount(), f2.getReservedAmount());
        assertTrue(r2.getUpdatedAt().isBefore(now));
    }
}
