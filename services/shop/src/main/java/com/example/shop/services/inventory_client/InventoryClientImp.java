package com.example.shop.services.inventory_client;

import com.example.grpc.inventory.stubs.InventoryServiceGrpc;
import com.example.grpc.inventory.stubs.ListProductReservationsRequest;
import com.example.shop.services.inventory_client.dto.ProductReservationResponse;
import com.example.shop.services.inventory_client.entities.ProductReservation;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class InventoryClientImp implements InventoryClient {

    @Autowired
    private InventoryServiceGrpc.InventoryServiceStub inventorySvcGRPC;

    @Override
    public Flux<ProductReservation> listProductReservations(String userId) {

        Sinks.Many<ProductReservation> sink = Sinks.many().multicast().onBackpressureBuffer();

        var request = ListProductReservationsRequest.newBuilder().setUserId(userId).build();

        inventorySvcGRPC.listProductReservations(request, new StreamObserver<>() {
            @Override
            public void onNext(com.example.grpc.inventory.stubs.ProductReservation data) {
                if (data != null) {
                    sink.emitNext(ProductReservationResponse.mapToEntity(data), Sinks.EmitFailureHandler.FAIL_FAST);
                }
            }

            @Override public void onError(Throwable t) {
                sink.emitError(t, Sinks.EmitFailureHandler.FAIL_FAST);
            }

            @Override public void onCompleted() {
                sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST); }
            });

        return sink.asFlux();
    }
}
