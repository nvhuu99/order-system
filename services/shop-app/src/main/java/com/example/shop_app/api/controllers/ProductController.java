package com.example.shop_app.api.controllers;

import com.example.grpc.inventory.stubs.ListProductsRequest;
import com.example.grpc.inventory.stubs.ProductServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/product")
public class ProductController {

    @Autowired
    private ProductServiceGrpc.ProductServiceBlockingStub productSvc;

    @GetMapping("products")
    public ResponseEntity<?> shop() {
        var request = ListProductsRequest.newBuilder().build();
        var result = productSvc.listProducts(request);
        return ResponseEntity.ok(result.getProductsList());
    }
}
