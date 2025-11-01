package com.example.inventory.services.product_availabilities;

import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProductAvailabilitiesService {

    Mono<ProductAvailability> syncWithReservations(String productId);

    Mono<List<ProductAvailability>> syncAllWithReservations(List<String> productIds);
}
