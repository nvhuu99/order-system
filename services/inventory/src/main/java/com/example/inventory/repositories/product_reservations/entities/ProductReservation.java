package com.example.inventory.repositories.product_reservations.entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("product_reservations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductReservation {
    @Id
    @Column("id")
    String id;

    @Column("user_id")
    String userId;

    @Column("product_id")
    String productId;

    @Column("reserved")
    Integer reserved = 0;

    @Column("desired_amount")
    Integer desiredAmount = 0;

    @Column("total_reserved_snapshot")
    Integer totalReservedSnapshot = 0;

    @Column("status")
    String status;

    @Column("expired_at")
    Instant expiredAt;

    @Column("updated_at")
    Instant updatedAt;
}
