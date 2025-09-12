package com.example.cart.services.cartmanager.entities;

import com.example.cart.entities.properties.CartAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@AllArgsConstructor
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartUpdateRequest {

    public record CartUpdateRequestEntry(
        String productId,
        String productName,
        Integer qtyAdjustment,
        CartAction action
    ) {}

    final String cartId;
    final Integer versionNumber;
    final Long timestamp;
    final List<CartUpdateRequestEntry> entries;

    public List<String> getProductIds() {
        return entries.stream().map(CartUpdateRequestEntry::productId).toList();
    }
}
