package com.example.shop.services.cart_service.entities;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    String userId;
    Map<String, CartItem> items;


    public Cart(String userId, List<CartItem> itemsList) {
        this.userId = userId;
        this.items = new HashMap<>();
        for (var item: itemsList) {
            this.items.put(item.getProductId(), item);
        }
    }
}
