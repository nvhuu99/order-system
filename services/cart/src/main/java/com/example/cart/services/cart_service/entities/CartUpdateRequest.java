package com.example.cart.services.cart_service.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    public enum CartAction {
        QTY_CHANGE,
        DROP_ITEM,
        ABANDON_CART
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CartUpdateRequestEntry {
        String productId;
        String productName;
        Integer qtyAdjustment;
        CartAction action;
    }

    String userId;
    Integer versionNumber;
    List<CartUpdateRequestEntry> entries;

    @JsonIgnore
    public List<String> getProductIds() {
        return entries.stream().map(CartUpdateRequestEntry::getProductId).toList();
    }
}
