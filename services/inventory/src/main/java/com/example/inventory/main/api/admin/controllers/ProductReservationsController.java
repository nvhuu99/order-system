package com.example.inventory.main.api.admin.controllers;

import com.example.inventory.main.api.admin.responses.ApiResponse;
import com.example.inventory.main.messaging.reservation_requests.ReservationRequestsTracker;
import com.example.inventory.services.product_reservations.ProductReservationsService;
import com.example.inventory.services.product_reservations.dto.ListRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("api/v1/admin/product-reservations")
public class ProductReservationsController {

    private static final Logger log = LoggerFactory.getLogger(ProductReservationsController.class);

    @Autowired
    private ProductReservationsService reservationsSvc;

    @Autowired
    private ReservationRequestsTracker tracker;

    @PostMapping("list")
    public Mono<ResponseEntity<ApiResponse>> list(@Valid @RequestBody ListRequest body) {
        return reservationsSvc
            .list(body)
            .collectList()
            .map(ApiResponse::ok)
            .doOnError(ex -> log.error("failed to list product reservations - {}", ex.getMessage()))
            .onErrorResume(ex ->  Mono.just(ApiResponse.internalServerError(null)))
        ;
    }

    @GetMapping("/reservation-requests-handled-total/{productId}")
    public Mono<ResponseEntity<ApiResponse>> reservationRequestsCounter(@PathVariable String productId) {
        return tracker
            .getTotalHandledRequests(productId)
            .map(value -> ApiResponse.ok(Map.of("handledTotal", value)))
            .doOnError(ex -> log.error("failed to get reservation-requests-handled-total - {}", ex.getMessage()))
            .onErrorResume(ex ->  Mono.just(ApiResponse.internalServerError(null)))
        ;
    }
}
