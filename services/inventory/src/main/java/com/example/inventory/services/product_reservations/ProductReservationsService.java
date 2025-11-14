package com.example.inventory.services.product_reservations;

import com.example.inventory.services.product_reservations.dto.ListRequest;
import com.example.inventory.services.product_reservations.dto.ProductReservationDetail;
import reactor.core.publisher.Flux;

public interface ProductReservationsService {
    Flux<ProductReservationDetail> list(ListRequest request);
}
