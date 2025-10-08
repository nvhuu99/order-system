package com.example.cart.services.inventory_service.drivers;

import com.example.cart.services.inventory_service.dto.ProductAvailabilityDTO;
import com.example.grpc.inventory.stubs.InventoryServiceGrpc;
import com.example.grpc.inventory.stubs.ListProductAvailabilitiesRequest;
import com.example.grpc.inventory.stubs.ProductAvailability;
import io.grpc.stub.StreamObserver;
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

@Service
public class InventoryService implements com.example.cart.services.inventory_service.InventoryService {

    @Autowired
    private InventoryServiceGrpc.InventoryServiceStub inventoryGRPC;

    @Override
    @Observed(name = "list_product_availabilities", lowCardinalityKeyValues = {
        "rpc.system", "grpc",
        "rpc.service", "inventory"
    })
    public Flux<com.example.cart.entities.ProductAvailability> listProductAvailabilities(List<String> productIds) {

        Sinks.Many<com.example.cart.entities.ProductAvailability> sink = Sinks.many().unicast().onBackpressureBuffer();

        var request = ListProductAvailabilitiesRequest.newBuilder()
            .addAllProductIds(productIds)
            .build();

        inventoryGRPC.listProductAvailabilities(request, new StreamObserver<>() {
            @Override
            public void onNext(ProductAvailability data) {
                sink.tryEmitNext(ProductAvailabilityDTO.map(data));
            }

            @Override
            public void onError(Throwable t) {
                sink.tryEmitError(t);
            }

            @Override
            public void onCompleted() {
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux();
    }
}
