package com.example.inventory.repositories.product_reservations.entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("product_reservations")
@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductReservation {
    @Id
    @Column("id")
    String id;

    @Column("product_id")
    String productId;

    @Column("quantity")
    Integer quantity;

    @Column("expired_at")
    Instant expiredAt;

    @Column("total_reserved_snapshot")
    Integer totalReservedSnapshot;

    @Column("status")
    String status;

    @Column("updated_at")
    Instant updatedAt;

    public ProductReservation(String reservationId) {
        id = reservationId;
    }
}
