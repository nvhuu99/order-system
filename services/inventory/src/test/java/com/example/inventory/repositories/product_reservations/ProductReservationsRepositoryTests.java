package com.example.inventory.repositories.product_reservations;

import com.example.inventory.TestBase;
import com.example.inventory.DatabaseSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductReservationsRepositoryTests extends TestBase {

    @Autowired
    R2dbcEntityTemplate r2dbcTemplate;

    @Autowired
    private DatabaseSeeder seeder;

    @Autowired
    private ProductReservationsRepository reservationsRepo;


    @Test
    void sumReservedAmountByProductIds_mustReturnCorrectSumForAllByProductIds_AndOnlyIncludeStatusOK_AndExcludesExpiredReservation() {
        var ids = List.of("prod01", "prod02", "prod03", "prod04", "prod05", "prod06", "prod07");
        var verifyIds = List.of("prod02", "prod03", "prod04", "prod06");
        var total = new AtomicInteger();

        seeder
            .seedProductsAndProductReservations(r2dbcTemplate)
            .thenMany(reservationsRepo.sumReservedAmountByProductIds(ids))
            .doOnNext(data -> {
                total.addAndGet(data.getReserved());
                assertTrue(verifyIds.contains(data.getProductId()));
            })
            .doOnComplete(() -> {
                assertEquals(total.get(), 10);
            })
            .blockLast()
        ;
    }
}
