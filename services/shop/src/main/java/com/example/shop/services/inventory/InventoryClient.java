package com.example.shop.services.inventory;

import com.example.shop.services.inventory.entities.ProductReservation;
import reactor.core.publisher.Flux;

public interface InventoryClient {
    Flux<ProductReservation> listProductReservations(String userId);
}
