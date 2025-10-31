package com.example.inventory.repositories.product_availabilities;

import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProductAvailabilitiesRepository {

    Mono<ProductAvailability> findByProductId(String productId);

    Mono<List<ProductAvailability>> findAllByProductIds(List<String> productIds);

    Mono<Void> save(ProductAvailability productAvailability);

    Mono<Void> saveMany(List<ProductAvailability> productAvailabilities);
}
