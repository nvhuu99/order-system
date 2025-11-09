package com.example.shop.services.cart_service.entities;

import com.example.shop.repositories.products.entities.Product;
import com.example.shop.services.inventory.entities.ProductReservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItem {
    private String productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer desiredAmount;
    private Integer reservedAmount;
    private String reservationStatus;

    public CartItem(ProductReservation reservation, Product product) {
        setProductId(product.getId());
        setProductName(product.getName());
        setProductPrice(product.getPrice());
        setReservedAmount(reservation.getReserved());
        setDesiredAmount(reservation.getDesiredAmount());
        setReservationStatus(reservation.getStatus());
    }
}
