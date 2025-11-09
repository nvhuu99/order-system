package com.example.inventory.main.grpc;

import com.example.grpc.inventory.stubs.InventoryServiceGrpc;
import com.example.grpc.inventory.stubs.ListProductReservationsRequest;
import com.example.grpc.inventory.stubs.ProductReservation;
import com.example.inventory.main.grpc.dto.ProductReservationResponse;
import com.example.inventory.repositories.product_reservations.ProductReservationsCrudRepository;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    @Autowired
    private ProductReservationsCrudRepository reservationsRepo;

    @Override
    public void listProductReservations(
        ListProductReservationsRequest request,
        StreamObserver<ProductReservation> responseObserver
    ) {
        reservationsRepo
            .findAllByUserId(request.getUserId())
            .doOnNext(reservation -> responseObserver.onNext(ProductReservationResponse.fromEntity(reservation)))
            .doOnComplete(responseObserver::onCompleted)
            .subscribe()
        ;
    }
}