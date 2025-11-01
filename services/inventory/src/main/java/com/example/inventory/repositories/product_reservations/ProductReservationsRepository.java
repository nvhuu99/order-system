package com.example.inventory.repositories.product_reservations;

import com.example.inventory.repositories.product_reservations.dto.ProductReservedAmount;
import com.example.inventory.repositories.product_reservations.entities.ProductReservation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ProductReservationsRepository extends ReactiveCrudRepository<ProductReservation, String> {
    @Query("""
        SELECT
            product_id,
            COALESCE(SUM(quantity), 0) AS reserved
        FROM product_reservations
        WHERE
            product_id IN (:productIds)
            AND status = 'OK'
            AND expired_at > CURRENT_TIMESTAMP
        GROUP BY product_id
    """)
    Flux<ProductReservedAmount> sumReservedAmountByProductIds(@Param("productIds") List<String> productIds);

    Mono<ProductReservation> findByProductIdAndUserId(String productId, String userId);
}
