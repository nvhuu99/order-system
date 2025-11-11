package com.example.inventory.services.products;

import com.example.inventory.services.products.dto.InsertProduct;
import com.example.inventory.services.products.dto.ListProductsRequest;
import com.example.inventory.services.products.dto.ProductDetail;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductsService {

    Mono<ProductDetail> findById(String id);

    Flux<ProductDetail> list(ListProductsRequest request);

    Mono<ProductDetail> insert(InsertProduct data);

    Mono<Void> deleteById(String id);
}
