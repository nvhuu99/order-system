package com.example.inventory.services;

import com.example.grpc.inventory.stubs.InventoryServiceGrpc;
import com.example.grpc.inventory.stubs.ListProductAvailabilitiesRequest;
import com.example.inventory.entities.ProductAvailability;
import com.example.inventory.services.dto.ProductAvailabilityDTO;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private static final List<ProductAvailability> inMemoryProducts = List.of(
        new ProductAvailability("P001", "Laptop", 1200.00, 10, true),
        new ProductAvailability("P002", "Smartphone", 800.00, 25, true),
        new ProductAvailability("P003", "Headphones", 150.00, 50, true),
        new ProductAvailability("P004", "Keyboard", 60.00, 40, true),
        new ProductAvailability("P005", "Mouse", 30.00, 45, true),
        new ProductAvailability("P006", "Monitor", 300.00, 20, true),
        new ProductAvailability("P007", "USB-C Cable", 15.00, 100, true),
        new ProductAvailability("P008", "External HDD", 90.00, 35, true),
        new ProductAvailability("P009", "Printer", 200.00, 12, true),
        new ProductAvailability("P010", "Tablet", 500.00, 18, true)
    );

    @Override
    public void listProductAvailabilities(
        ListProductAvailabilitiesRequest request,
        StreamObserver<com.example.grpc.inventory.stubs.ProductAvailability> responseObserver
    ) {
        inMemoryProducts.stream()
            .filter(p -> request.getProductIdsList().contains(p.getProductId()))
            .forEach(p -> {
                responseObserver.onNext(ProductAvailabilityDTO.fromEntity(p));
            });

        responseObserver.onCompleted();
    }
}