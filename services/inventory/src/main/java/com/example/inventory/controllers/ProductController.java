package com.example.inventory.controllers;

import com.example.grpc.inventory.stubs.ListProductsRequest;
import com.example.grpc.inventory.stubs.ListProductsResponse;
import com.example.grpc.inventory.stubs.Product;
import com.example.grpc.inventory.stubs.ProductServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class ProductController extends ProductServiceGrpc.ProductServiceImplBase {

    @Override
    public void listProducts(ListProductsRequest req, StreamObserver<ListProductsResponse> respObs) {
        ListProductsResponse resp = ListProductsResponse.newBuilder().addProducts(
            Product.newBuilder()
                .setId(1)
                .setName("Product 1")
                .setPrice(0.5)
                .build()
        ).build();
        respObs.onNext(resp);
        respObs.onCompleted();
    }
}