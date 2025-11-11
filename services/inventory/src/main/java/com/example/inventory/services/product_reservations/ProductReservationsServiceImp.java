package com.example.inventory.services.product_reservations;

import com.example.inventory.main.grpc.dto.ProductReservationResponse;
import com.example.inventory.repositories.product_reservations.ProductReservationsCrudRepository;
import com.example.inventory.services.product_reservations.dto.ListProductReservationsRequest;
import com.example.inventory.services.product_reservations.dto.ProductReservationDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ProductReservationsServiceImp implements ProductReservationsService{

    @Autowired
    private ProductReservationsCrudRepository reservationsRepo;


    @Override
    public Flux<ProductReservationDetail> list(ListProductReservationsRequest request) {
        return reservationsRepo
            .findAllByUserId(request.getUserId())
            .map(ProductReservationDetail::mapFromEntity)
        ;
    }
}
