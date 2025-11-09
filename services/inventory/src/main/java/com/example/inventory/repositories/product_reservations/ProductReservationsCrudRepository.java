package com.example.inventory.repositories.product_reservations;

import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductReservationsCrudRepository extends ReactiveCrudRepository<ProductReservation, String> {

    Flux<ProductReservation> findAllByUserId(String userId);

    Mono<ProductReservation> findByProductIdAndUserId(String productId, String userId);
}
