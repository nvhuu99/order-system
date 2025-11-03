package com.example.inventory.repositories.products.entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.relational.core.mapping.Column;

@Table("products")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
    @Id
    @Column("id")
    String id;

    @Column("name")
    String name;

    @Column("price")
    BigDecimal price;

    @Column("stock")
    Integer stock;

    @Column("updated_at")
    Instant updatedAt;
}
