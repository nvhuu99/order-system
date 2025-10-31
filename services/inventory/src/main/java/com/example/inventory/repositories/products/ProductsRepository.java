package com.example.inventory.repositories.products;

import com.example.inventory.repositories.products.entities.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ProductsRepository extends ReactiveCrudRepository<Product, String> {
}
