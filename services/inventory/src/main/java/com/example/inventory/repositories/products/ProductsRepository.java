package com.example.inventory.repositories.products;

import com.example.inventory.repositories.products.dto.ListProductsRequest;
import com.example.inventory.repositories.products.entities.Product;
import reactor.core.publisher.Flux;

public interface ProductsRepository {

    Flux<Product> list(ListProductsRequest request);
}
