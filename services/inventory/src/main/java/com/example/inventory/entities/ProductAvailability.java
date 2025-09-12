package com.example.inventory.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductAvailability {
    String productId;
    String productName;
    Double price;
    Integer availableStock;
    Boolean isAvailable;
}
