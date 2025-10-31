package com.example.inventory.repositories.product_reservations.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductReservedAmount {

    String productId;

    Integer reserved;
}
