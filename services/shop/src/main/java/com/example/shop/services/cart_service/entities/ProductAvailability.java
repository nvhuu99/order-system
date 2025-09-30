package com.example.shop.services.cart_service.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductAvailability {
    String productId;
    String productName;
    Double price;
    Integer availableStock;
    Boolean isAvailable;
}
