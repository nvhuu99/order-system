package com.example.inventory.services.product_reservations;

import com.example.inventory.repositories.product_reservations.ProductReservationsRepository;
import com.example.inventory.services.product_reservations.dto.ListRequest;
import com.example.inventory.services.product_reservations.dto.ProductReservationDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ProductReservationsServiceImp implements ProductReservationsService{

    @Autowired
    private ProductReservationsRepository reservationsRepo;


    @Override
    public Flux<ProductReservationDetail> list(ListRequest request) {
        return reservationsRepo
            .list(request)
            .map(ProductReservationDetail::mapFromEntity)
        ;
    }
}
