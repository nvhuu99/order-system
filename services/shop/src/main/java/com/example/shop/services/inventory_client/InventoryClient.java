package com.example.shop.services.inventory_client;

import com.example.shop.services.inventory_client.entities.ProductReservation;
import reactor.core.publisher.Flux;

public interface InventoryClient {
    Flux<ProductReservation> listProductReservations(String userId);
}
