package com.example.cart.entities;

import lombok.Data;

@Data
public class ProductAvailability {
    String productId;
    String productName;
    Double price;
    Integer availableStock;
    Boolean isAvailable;
}
