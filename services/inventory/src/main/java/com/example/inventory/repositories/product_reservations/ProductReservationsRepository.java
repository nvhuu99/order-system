package com.example.inventory.repositories.product_reservations;

import com.example.inventory.repositories.product_reservations.dto.ListRequest;
import com.example.inventory.repositories.product_reservations.dto.ProductReservedAmount;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProductReservationsRepository {

    Flux<ProductReservation> list(ListRequest request);

    Flux<ProductReservedAmount> sumReservedAmounts(List<String> productIds);

    Mono<Void> syncReservations(List<String> productIds);
    
    Mono<Void> removeZeroAmountReservations(List<String> productIds);
}
