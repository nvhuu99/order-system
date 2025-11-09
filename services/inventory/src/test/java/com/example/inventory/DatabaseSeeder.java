package com.example.inventory;

import com.example.inventory.repositories.product_reservations.entities.ReservationStatus;
import com.example.inventory.repositories.products.entities.Product;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class DatabaseSeeder {

    @Autowired
    private R2dbcEntityTemplate template;


    public Product seedProduct(String id, Integer stock) {
        return template
            .insert(new Product(id, id, BigDecimal.valueOf(1 + 10 * Math.random()), stock, Instant.now()))
            .block();
    }

    public ProductReservation seedReservation(String productId, String usrId, Integer reserved, Integer desired, ReservationStatus status, Integer updatedAtAdditionalHours) {
        var now = Instant.now();
        var r = new ProductReservation(
            UUID.randomUUID().toString(),
            usrId,
            productId,
            reserved,
            desired,
            status.getValue(),
            status == ReservationStatus.EXPIRED ? now.minus(2, ChronoUnit.HOURS) : now.plus(2, ChronoUnit.HOURS),
            now.plusSeconds(updatedAtAdditionalHours * 60 * 60)
        );
        return template.insert(r).block();
    }
}
