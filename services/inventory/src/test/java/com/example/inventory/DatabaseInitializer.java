package com.example.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Files;

@Component
public class DatabaseInitializer {

    @Autowired
    private R2dbcEntityTemplate r2dbcTemplate;

    @Value("classpath:database/create-products.sql")
    private Resource CREATE_PRODUCTS;

    @Value("classpath:database/create-product-reservations.sql")
    private Resource CREATE_PRODUCT_RESERVATIONS;

    public Mono<Void> createTables() {
        try {
            var db = r2dbcTemplate.getDatabaseClient();
            var createProductTable = db.sql(Files.readString(CREATE_PRODUCTS.getFile().toPath())).fetch().rowsUpdated();
            var createReservationTable = db.sql(Files.readString(CREATE_PRODUCT_RESERVATIONS.getFile().toPath())).fetch().rowsUpdated();

            return createProductTable
                .doOnSuccess(ok -> System.out.println("created table products"))
                .then(createReservationTable)
                .doOnSuccess(ok -> System.out.println("created table product_reservations"))
                .then()
            ;
        } catch (Exception ex) {
            return Mono.empty();
        }
    }
}

