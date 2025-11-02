package com.example.inventory.repositories.product_availabilities;

import com.example.inventory.DatabaseSeeder;
import com.example.inventory.TestBase;
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
    private ProductAvailabilitiesRepository productAvailabilitiesRepo;

    @Autowired
    private ProductAvailabilitiesService productAvailabilitiesService;

    private void setup() {
        seeder
            .seedProductsAndProductReservations()
            .block()
        ;
        productAvailabilitiesService
            .syncAllWithReservations(List.of("prod01", "prod02", "prod03", "prod04", "prod05", "prod06", "prod07"))
            .block()
        ;
    }

    @Test
    public void findByProductId_mustReturnData_whenExists() {
        setup();

        var p1 = productAvailabilitiesRepo.findByProductId("prod01").block();
        var p3 = productAvailabilitiesRepo.findByProductId("prod03").block();

        assertNotNull(p1);
        assertNotNull(p3);

        assertEquals(p1.getProductId(), "prod01");
        assertEquals(p1.getReserved(), 0);
        assertEquals(p1.getStock(), 0);
        assertNotNull(p1.getUpdatedAt());

        assertEquals(p3.getProductId(), "prod03");
        assertEquals(p3.getReserved(), 7);
        assertEquals(p3.getStock(), 7);
        assertNotNull(p3.getUpdatedAt());
    }

    @Test
    public void findAllByProductIds_mustReturnData_whenExists() {
        setup();

        var results = productAvailabilitiesRepo.findAllByProductIds(List.of("prod04", "prod05")).block();

        assertEquals(results.size(), 2);

        var p4 = results.get(0);
        var p5 = results.get(1);

        assertNotNull(p4);
        assertNotNull(p5);

        assertEquals(p4.getProductId(), "prod04");
        assertEquals(p4.getReserved(), 3);
        assertEquals(p4.getStock(), 5);
        assertNotNull(p4.getUpdatedAt());

        assertEquals(p5.getProductId(), "prod05");
        assertEquals(p5.getReserved(), 0);
        assertEquals(p5.getStock(), 2);
        assertNotNull(p5.getUpdatedAt());
    }

    @Test
    public void save_mustSuccess() {
        setup();

        var data = new ProductAvailability();
        data.setProductId(UUID.randomUUID().toString());
        data.setStock(4);
        data.setReserved(2);
        data.setUpdatedAt(Instant.now());

        productAvailabilitiesRepo.save(data).block();
        var findResult = productAvailabilitiesRepo.findByProductId(data.getProductId()).block();

        assertNotNull(findResult);
        assertEquals(data.getProductId(), findResult.getProductId());
        assertEquals(data.getStock(), findResult.getStock());
        assertEquals(data.getReserved(), findResult.getReserved());
        assertNotNull(findResult.getUpdatedAt());
    }

    @Test
    public void saveMany_mustSuccess() {
        setup();

        var r1 = new ProductAvailability();
        r1.setProductId(UUID.randomUUID().toString());
        r1.setStock(4);
        r1.setReserved(2);
        r1.setUpdatedAt(Instant.now());

        var r2 = new ProductAvailability();
        r2.setProductId(UUID.randomUUID().toString());
        r2.setStock(6);
        r2.setReserved(1);
        r2.setUpdatedAt(Instant.now());

        productAvailabilitiesRepo.saveMany(List.of(r1, r2)).block();
        var f1 = productAvailabilitiesRepo.findByProductId(r1.getProductId()).block();
        var f2 = productAvailabilitiesRepo.findByProductId(r2.getProductId()).block();

        assertNotNull(f1);
        assertEquals(r1.getProductId(), f1.getProductId());
        assertEquals(r1.getStock(), f1.getStock());
        assertEquals(r1.getReserved(), f1.getReserved());
        assertNotNull(r1.getUpdatedAt());

        assertNotNull(f2);
        assertEquals(r2.getProductId(), f2.getProductId());
        assertEquals(r2.getStock(), f2.getStock());
        assertEquals(r2.getReserved(), f2.getReserved());
        assertNotNull(r2.getUpdatedAt());
    }
}
