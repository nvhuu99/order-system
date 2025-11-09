package com.example.shop.services.cart_service.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartUpdateRequest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CartUpdateRequestEntry {
        String productId;
        Integer quantity;
    }

    String userId;
    Integer versionNumber;
    List<CartUpdateRequestEntry> entries;
}
