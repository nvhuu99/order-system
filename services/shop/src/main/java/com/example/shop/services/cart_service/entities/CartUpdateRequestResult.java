package com.example.shop.services.cart_service.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartUpdateRequestResult {
    private Boolean success;
    private String message;
}
