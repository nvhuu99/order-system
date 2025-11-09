package com.example.shop.repositories.products;

import com.example.shop.repositories.products.entities.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ProductsRepository extends ReactiveCrudRepository<Product, String> {
}
