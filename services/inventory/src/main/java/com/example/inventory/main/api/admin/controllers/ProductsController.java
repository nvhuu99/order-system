package com.example.inventory.main.api.admin.controllers;

import com.example.inventory.main.api.admin.responses.ApiResponse;
import com.example.inventory.services.products.ProductsService;
import com.example.inventory.services.products.dto.InsertProduct;
import com.example.inventory.services.products.dto.ListRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/admin/products")
public class ProductsController {

    private static final Logger log = LoggerFactory.getLogger(ProductsController.class);

    @Autowired
    private ProductsService productsSvc;

    @GetMapping("{id}")
    public Mono<ResponseEntity<ApiResponse>> getProduct(@PathVariable String id) {
        return productsSvc
            .findById(id)
            .map(ApiResponse::ok)
            .switchIfEmpty(Mono.just(ApiResponse.notFound()))
            .doOnError(ex -> log.error(ex.getMessage()))
            .onErrorResume(ex ->  Mono.just(ApiResponse.internalServerError(null)))
        ;
    }

    @PostMapping("list")
    public Mono<ResponseEntity<ApiResponse>> listProduct(@RequestBody ListRequest request) {
        return productsSvc
            .list(request)
            .collectList()
            .map(ApiResponse::ok)
            .switchIfEmpty(Mono.just(ApiResponse.notFound()))
            .doOnError(ex -> log.error(ex.getMessage()))
            .onErrorResume(ex ->  Mono.just(ApiResponse.internalServerError(null)))
        ;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse>> insertProduct(@Valid @RequestBody InsertProduct data) {
        return productsSvc
            .insert(data)
            .map(ApiResponse::created)
            .doOnError(ex -> log.error(ex.getMessage()))
            .onErrorResume(ex ->  Mono.just(ApiResponse.internalServerError(null)))
        ;
    }

    @DeleteMapping("{id}")
    public Mono<ResponseEntity<ApiResponse>> deleteProduct(@PathVariable String id) {
        return productsSvc
            .deleteById(id)
            .map(ApiResponse::ok)
            .doOnError(ex -> log.error(ex.getMessage()))
            .onErrorResume(ex ->  Mono.just(ApiResponse.internalServerError(null)))
        ;
    }
}
