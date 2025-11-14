package com.example.inventory.main.api.admin.controllers;

import com.example.inventory.main.api.admin.responses.ApiResponse;
import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.services.product_reservations.ProductReservationsService;
import com.example.inventory.services.product_reservations.dto.ListRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/admin/product-availabilities")
public class ProductAvailabilitiesController {

    private static final Logger log = LoggerFactory.getLogger(ProductAvailabilitiesController.class);

    @Autowired
    private ProductAvailabilitiesRepository availabilitiesRepo;

    @GetMapping("{productId}")
    public Mono<ResponseEntity<ApiResponse>> findByProductId(@PathVariable String productId) {
        return availabilitiesRepo
            .findByProductId(productId)
            .map(ApiResponse::ok)
            .doOnError(ex -> log.error(ex.getMessage()))
            .onErrorResume(ex ->  Mono.just(ApiResponse.internalServerError(null)))
        ;
    }
}
