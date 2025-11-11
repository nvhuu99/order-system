package com.example.inventory.repositories.products;

import com.example.inventory.repositories.products.entities.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProductsCrudRepository extends ReactiveCrudRepository<Product, String> {
    @Query("SELECT id FROM products")
    Flux<String> getProductListIds(Pageable pagination);
}
