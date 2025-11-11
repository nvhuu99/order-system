package com.example.inventory.main.api.admin.controllers;

import com.example.inventory.main.api.admin.responses.ApiResponse;
import com.example.inventory.services.product_reservations.ProductReservationsService;
import com.example.inventory.services.product_reservations.dto.ListProductReservationsRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/admin/product-reservations")
public class ProductReservationController {

    private static final Logger log = LoggerFactory.getLogger(ProductReservationController.class);

    @Autowired
    private ProductReservationsService reservationsSvc;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse>> list(@Valid @RequestBody ListProductReservationsRequest body) {
        return reservationsSvc
            .list(body)
            .collectList()
            .map(ApiResponse::ok)
            .doOnError(ex -> log.error(ex.getMessage()))
            .onErrorResume(ex ->  Mono.just(ApiResponse.internalServerError(null)))
        ;
    }
}
