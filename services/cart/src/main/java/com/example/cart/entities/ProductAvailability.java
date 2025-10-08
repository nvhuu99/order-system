package com.example.cart.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductAvailability {
    String productId;
    String productName;
    Double price;
    Integer availableStock;
    Boolean isAvailable;
}
