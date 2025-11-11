package com.example.inventory.services.products;

import com.example.inventory.repositories.products.ProductsCrudRepository;
import com.example.inventory.repositories.products.ProductsRepository;
import com.example.inventory.repositories.products.entities.Product;
import com.example.inventory.services.products.dto.InsertProduct;
import com.example.inventory.services.products.dto.ListProductsRequest;
import com.example.inventory.services.products.dto.ProductDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class ProductsServiceImp implements ProductsService {

    @Autowired
    private ProductsCrudRepository productsCrudRepo;

    @Autowired
    private ProductsRepository productsRepo;

    @Override
    public Mono<ProductDetail> findById(String id) {
        return productsCrudRepo.findById(id).map(ProductDetail::mapFromEntity);
    }

    @Override
    public Flux<ProductDetail> list(ListProductsRequest request) {
        return productsRepo.list(request).map(ProductDetail::mapFromEntity);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return productsCrudRepo.deleteById(id).then();
    }

    @Override
    public Mono<ProductDetail> insert(InsertProduct data) {
        var product = new Product();
        product.setName(data.getName());
        product.setPrice(data.getPrice());
        product.setStock(data.getStock());
        product.setUpdatedAt(Instant.now());
        return productsCrudRepo.save(product).map(ProductDetail::mapFromEntity);
    }
}
