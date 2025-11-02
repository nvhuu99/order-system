package com.example.inventory;

import com.example.inventory.enums.ReservationStatus;
import com.example.inventory.repositories.products.entities.Product;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DatabaseSeeder {

    @Autowired
    private DatabaseInitializer db;

    @Autowired
    private R2dbcEntityTemplate template;

    public static List<Product> seedProducts = new ArrayList<>();
    public static List<ProductReservation> seedReservations = new ArrayList<>();

    private static final AtomicBoolean hasSeederRun = new AtomicBoolean();

    public Mono<Void> seedProductsAndProductReservations() {
        /*
        USERS:
        - usr01, usr02

        RESERVATION SUMMARY:
        - prod01: stock = 0, reserved amount = 0 - reservations: INSUFFICIENT_STOCK = 1
        - prod02: stock = 3, reserved amount = 1 - reservations: OK = 1.
        - prod03: stock = 7, reserved amount = 7 - reservations: OK = (2, 3). INSUFFICIENT_STOCK = 3
        - prod04: stock = 5, reserved amount = 3 - OK = (1, 1, 1). EXPIRED = 1
        - prod05: stock = 2, reserved amount = 0 - EXPIRED = 1
        - prod06 (usr02): stock = 5, reserved amount = 1 - OK = 1.
        - prod07: stock = 5 - no reservation
        */

        if (hasSeederRun.get()) {
            return Mono.empty();
        }

        seedProducts.add(newProduct("prod01", 0));
        seedProducts.add(newProduct("prod02", 3));
        seedProducts.add(newProduct("prod03", 7));
        seedProducts.add(newProduct("prod04", 5));
        seedProducts.add(newProduct("prod05", 2));
        seedProducts.add(newProduct("prod06", 5));
        seedProducts.add(newProduct("prod07", 5));

        seedReservations.add(newReservation("prod01", "usr01", 1, 0, 0, ReservationStatus.INSUFFICIENT_STOCK));
        seedReservations.add(newReservation("prod02", "usr01", 1, 1, 1, ReservationStatus.OK));
        seedReservations.add(newReservation("prod03", "usr01", 2, 2, 2, ReservationStatus.OK));
        seedReservations.add(newReservation("prod03", "usr01", 3, 3, 5, ReservationStatus.OK));
        seedReservations.add(newReservation("prod03", "usr01", 3, 2, 5, ReservationStatus.INSUFFICIENT_STOCK));
        seedReservations.add(newReservation("prod04", "usr01", 1, 1, 1, ReservationStatus.OK));
        seedReservations.add(newReservation("prod04", "usr01", 1, 1, 2, ReservationStatus.OK));
        seedReservations.add(newReservation("prod04", "usr01", 1, 1, 3, ReservationStatus.OK));
        seedReservations.add(newReservation("prod04", "usr01", 1, 0, 1, ReservationStatus.EXPIRED));
        seedReservations.add(newReservation("prod05", "usr01", 1, 0, 0, ReservationStatus.EXPIRED));
        seedReservations.add(newReservation("prod06", "usr02", 1, 1, 1, ReservationStatus.OK));

        return db.createTables()
            .then(template.delete(ProductReservation.class).all())
            .then(template.delete(Product.class).all())
            .thenMany(Flux.fromIterable(seedProducts))
            .flatMap(template::insert)
            .thenMany(Flux.fromIterable(seedReservations))
            .flatMap(template::insert)
            .doOnComplete(() -> System.out.println("seeder run completely"))
            .doOnComplete(() -> hasSeederRun.set(true))
            .then()
        ;
    }

    private static Product newProduct(String id, Integer stock) {
        var minPrice = 1;
        var maxPrice = 10;
        Product p = new Product();
        p.setId(id);
        p.setName(id);
        p.setPrice(BigDecimal.valueOf(minPrice + maxPrice * Math.random()));
        p.setStock(stock);
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private static ProductReservation newReservation(
        String productId,
        String usrId,
        Integer desire,
        Integer reserved,
        Integer snapshot,
        ReservationStatus status
    ) {
        var now = Instant.now();
        var notExpired = now.plus(2, ChronoUnit.HOURS);
        var expired = now.minus(2, ChronoUnit.HOURS);
        ProductReservation r = new ProductReservation();
        r.setId(UUID.randomUUID().toString());
        r.setUserId(usrId);
        r.setProductId(productId);
        r.setDesiredAmount(desire);
        r.setReserved(reserved);
        if (status == ReservationStatus.EXPIRED) {
            r.setExpiredAt(expired);
        } else {
            r.setExpiredAt(notExpired);
        }
        r.setTotalReservedSnapshot(snapshot);
        r.setStatus(status.getValue());
        r.setUpdatedAt(now);
        return r;
    }
}
