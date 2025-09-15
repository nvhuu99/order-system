package com.example.cart.services.inventory_service;

import com.example.cart.entities.ProductAvailability;
import reactor.core.publisher.Flux;

import java.util.List;

public interface InventoryService {
    Flux<ProductAvailability> listProductAvailabilities(List<String> productIds);
}
