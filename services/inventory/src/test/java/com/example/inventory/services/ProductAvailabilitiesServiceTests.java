package com.example.inventory.services;

import com.example.inventory.DatabaseSeeder;
import com.example.inventory.TestBase;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.services.product_availabilities.ProductAvailabilitiesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductAvailabilitiesServiceTests extends TestBase {

    @Autowired
    private DatabaseSeeder seeder;

    @Autowired
    private ProductAvailabilitiesRepository productAvailabilitiesRepo;

    @Autowired
    private ProductAvailabilitiesService productAvailabilitiesService;


    @Test
    void syncWithReservations_afterSync_dataShouldMatch() {
        seeder
            .seedProductsAndProductReservations()
            .then(productAvailabilitiesService.syncWithReservations("prod02"))
            .then(productAvailabilitiesService.syncWithReservations("prod03"))
            .then(productAvailabilitiesService.syncWithReservations("prod04"))
            .block()
        ;

        var r2 = productAvailabilitiesRepo.findByProductId("prod02").block();
        var r3 = productAvailabilitiesRepo.findByProductId("prod03").block();
        var r4 = productAvailabilitiesRepo.findByProductId("prod04").block();

        assertNotNull(r2);
        assertNotNull(r3);
        assertNotNull(r4);

        assertEquals(r2.getReserved(), 1);
        assertEquals(r3.getReserved(), 5);
        assertEquals(r4.getReserved(), 3);
    }

    @Test
    void syncAllWithReservations_afterSync_dataShouldMatch() {
        seeder
            .seedProductsAndProductReservations()
            .then(productAvailabilitiesService.syncAllWithReservations(List.of("prod01", "prod05", "prod06")))
            .block()
        ;

        var r1 = productAvailabilitiesRepo.findByProductId("prod01").block();
        var r5 = productAvailabilitiesRepo.findByProductId("prod05").block();
        var r6 = productAvailabilitiesRepo.findByProductId("prod06").block();

        assertNotNull(r1);
        assertNotNull(r5);
        assertNotNull(r6);

        assertEquals(r1.getReserved(), 0);
        assertEquals(r5.getReserved(), 0);
        assertEquals(r6.getReserved(), 1);
    }
}
