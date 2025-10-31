package com.example.inventory.repositories.products.entities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.relational.core.mapping.Column;

@Table("products")
@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
    @Id
    @Column("id")
    String id;

    @Column("price")
    BigDecimal price;

    @Column("name")
    String name;

    @Column("stock")
    Integer stock;

    @Column("updated_at")
    Instant updatedAt;

    public Product() {
        this.id = UUID.randomUUID().toString();
    }
}
