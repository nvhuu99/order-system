package com.example.cart.grpc.mappers;


import com.example.cart.services.cart_service.entities.CartUpdateRequest;

import java.util.ArrayList;

public class CartUpdateRequestMapper {

    public static CartUpdateRequest mapToEntity(com.example.grpc.cart.stubs.CartUpdateRequest data) {
        if (data == null) {
            return null;
        }
        var entries = new ArrayList<CartUpdateRequest.CartUpdateRequestEntry>();
        for (var entryDTO : data.getEntriesList()) {
            entries.add(new CartUpdateRequest.CartUpdateRequestEntry(
                entryDTO.getProductId(),
                entryDTO.getProductName(),
                entryDTO.getQtyAdjustment(),
                CartUpdateRequest.CartAction.valueOf(entryDTO.getAction().toString())
            ));
        }

        var entity = new CartUpdateRequest();
        entity.setUserId(data.getUserId());
        entity.setVersionNumber(data.getVersionNumber());
        entity.setEntries(entries);

        return entity;
    }
}
