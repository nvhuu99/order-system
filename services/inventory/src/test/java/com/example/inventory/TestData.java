package com.example.inventory;

import com.example.inventory.enums.ReservationStatus;
import com.example.inventory.repositories.products.entities.Product;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestData {

    public static Mono<Void> seedProductsAndProductReservations(R2dbcEntityTemplate template) {
        /*
        USERS: usr01, usr02
        PRODUCTS SUMMARY
        - prod01: stock = 0     prod02: stock = 3       prod03: stock = 7
        - prod04: stock = 5     prod05: stock = 2       prod06: stock = 5
        - prod07: stock = 5
        PRODUCT_RESERVATIONS SUMMARY
        - prod01: reserved amount = 0. INSUFFICIENT_STOCK = 1
        - prod02: reserved amount = 1. OK = 1.
        - prod03: reserved amount = 5. OK = (2, 3). INSUFFICIENT_STOCK = 3
        - prod04: reserved amount = 3. OK = (1, 1, 1). EXPIRED = 1
        - prod05: reserved amount = 0. EXPIRED = 1
        - prod06 (usr02): reserved amount = 1. OK = 1.
        - prod07: no reservation
        */
        List<Product> products = new ArrayList<>();
        List<ProductReservation> reservations = new ArrayList<>();

        products.add(newProduct("prod01", 0));
        products.add(newProduct("prod02", 3));
        products.add(newProduct("prod03", 7));
        products.add(newProduct("prod04", 5));
        products.add(newProduct("prod05", 2));
        products.add(newProduct("prod06", 5));
        products.add(newProduct("prod07", 5));

        reservations.add(newReservation("prod01", "usr01", 1, 0, ReservationStatus.INSUFFICIENT_STOCK));

        reservations.add(newReservation("prod02", "usr01", 1, 1, ReservationStatus.OK));

        reservations.add(newReservation("prod03", "usr01", 2, 2, ReservationStatus.OK));
        reservations.add(newReservation("prod03", "usr01", 3, 5, ReservationStatus.OK));
        reservations.add(newReservation("prod03", "usr01", 3, 5, ReservationStatus.INSUFFICIENT_STOCK));

        reservations.add(newReservation("prod04", "usr01", 1, 1, ReservationStatus.OK));
        reservations.add(newReservation("prod04", "usr01", 1, 2, ReservationStatus.OK));
        reservations.add(newReservation("prod04", "usr01", 1, 3, ReservationStatus.OK));
        reservations.add(newReservation("prod04", "usr01", 1, 3, ReservationStatus.EXPIRED));

        reservations.add(newReservation("prod05", "usr01", 1, 0, ReservationStatus.EXPIRED));

        reservations.add(newReservation("prod06", "usr02", 1, 1, ReservationStatus.OK));

        return template.delete(ProductReservation.class).all()
            .then(template.delete(Product.class).all())
            .thenMany(Flux.fromIterable(products))
            .flatMap(template::insert)
            .thenMany(Flux.fromIterable(reservations))
            .flatMap(template::insert)
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

    private static ProductReservation newReservation(String productId, String usrId, Integer qty, Integer snapshot, ReservationStatus status) {
        var now = Instant.now();
        var notExpired = now.plus(2, ChronoUnit.HOURS);
        var expired = now.minus(2, ChronoUnit.HOURS);
        ProductReservation r = new ProductReservation();
        r.setId(UUID.randomUUID().toString());
        r.setUserId(usrId);
        r.setProductId(productId);
        r.setQuantity(qty);
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
