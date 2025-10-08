package com.example.cart.services.cart_service.mocks;

import com.example.cart.entities.ProductAvailability;
import com.example.cart.services.inventory_service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@Profile("test")
public class InventoryServiceMock implements InventoryService {

    @Autowired
    private Map<String, ProductAvailability> productAvailabilities;

    public Flux<ProductAvailability> listProductAvailabilities(List<String> productIds) {
        var matches = productAvailabilities
            .values()
            .stream()
            .filter(prod -> productIds.contains(prod.getProductId()))
            .toList()
        ;
        return Flux.fromIterable(matches);
    }
}
