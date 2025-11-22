package com.example.inventory.main.grpc;

import com.example.grpc.inventory.stubs.InventoryServiceGrpc;
import com.example.grpc.inventory.stubs.ListProductReservationsRequest;
import com.example.grpc.inventory.stubs.ProductReservation;
import com.example.inventory.main.grpc.dto.ProductReservationResponse;
import com.example.inventory.repositories.product_reservations.ProductReservationsCrudRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(InventoryGrpcService.class);
    @Autowired
    private ProductReservationsCrudRepository reservationsRepo;

    @Override
    public void listProductReservations(
        ListProductReservationsRequest request,
        StreamObserver<ProductReservation> responseObserver
    ) {
        reservationsRepo
            .findAllByUserId(request.getUserId())
            .doOnNext(reservation -> {
                responseObserver.onNext(ProductReservationResponse.fromEntity(reservation));
            })
            .doOnComplete(() -> responseObserver.onCompleted())
            .doOnError(ex -> {
                log.error("failed to list product reservations - {}", ex.getMessage());
                responseObserver.onError(Status.INTERNAL.withDescription(ex.getMessage()).withCause(ex).asRuntimeException());
            })
            .subscribe()
        ;
    }
}