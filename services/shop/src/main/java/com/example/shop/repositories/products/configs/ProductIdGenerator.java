package com.example.shop.repositories.products.configs;

import com.example.shop.repositories.products.entities.Product;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class ProductIdGenerator implements BeforeConvertCallback<Product> {

    @Override
    public Publisher<Product> onBeforeConvert(Product entity, SqlIdentifier table) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        return Mono.just(entity);
    }
}
