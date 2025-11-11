package com.example.inventory.services.products.dto;


import com.example.inventory.repositories.products.entities.Product;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDetail {

    String id;

    String name;

    BigDecimal price;

    Integer stock;

    Instant updatedAt;


    public static ProductDetail mapFromEntity(Product product) {
        var detail = new ProductDetail();
        detail.setId(product.getId());
        detail.setName(product.getName());
        detail.setPrice(product.getPrice());
        detail.setStock(product.getStock());
        detail.setUpdatedAt(product.getUpdatedAt());
        return detail;
    }
}
