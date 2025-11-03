package com.example.inventory.repositories.product_reservations.configs;

import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class ReservationIdGenerator implements BeforeConvertCallback<ProductReservation> {

    @Override
    public Publisher<ProductReservation> onBeforeConvert(ProductReservation entity, SqlIdentifier table) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        return Mono.just(entity);
    }
}
