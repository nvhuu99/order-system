package com.example.shop.services.cart_service.drivers.mappers;

import com.example.shop.services.cart_service.entities.CartUpdateRequest;

public class CartUpdateRequestMapper {

    public static com.example.grpc.cart.stubs.CartUpdateRequest mapFromEntity(CartUpdateRequest data) {
        var requestBuilder = com.example.grpc.cart.stubs.CartUpdateRequest.newBuilder()
            .setUserId(data.getUserId())
            .setVersionNumber(data.getVersionNumber());

        for (var entry : data.getEntries()) {
            var mappedEntryBuilder = com.example.grpc.cart.stubs.CartUpdateRequest.CartUpdateRequestEntry
                .newBuilder()
                .setAction(com.example.grpc.cart.stubs.CartUpdateRequest.CartAction.valueOf(entry.getAction().toString()))
                .setProductId(entry.getProductId())
                .setProductName(entry.getProductName())
                .setQtyAdjustment(entry.getQtyAdjustment())
                ;
            requestBuilder.addEntries(mappedEntryBuilder.build());
        }

        return requestBuilder.build();
    }
}
