package com.example.cart.services.inventory;

import com.example.cart.entities.ProductAvailability;
import reactor.core.publisher.Flux;

public interface InventoryService {
    Flux<ProductAvailability> listProductAvailabilities(Iterable<String> productIds);
}
