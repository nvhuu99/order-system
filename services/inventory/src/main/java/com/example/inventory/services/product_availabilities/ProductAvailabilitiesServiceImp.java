package com.example.inventory.services.product_availabilities;

import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import com.example.inventory.repositories.product_reservations.ProductReservationsRepository;
import com.example.inventory.repositories.product_reservations.dto.ProductReservedAmount;
import com.example.inventory.repositories.products.ProductsCrudRepository;
import com.example.inventory.repositories.products.entities.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
public class ProductAvailabilitiesServiceImp implements ProductAvailabilitiesService {

    @Autowired
    private ProductReservationsRepository reservationsRepo;

    @Autowired
    private ProductsCrudRepository productsRepo;

    @Autowired
    private ProductAvailabilitiesRepository productAvailabilitiesRepo;


    public Mono<ProductAvailability> syncWithReservations(String productId) {
        var productResult = productsRepo.findById(productId);
        var reservedResult = reservationsRepo
            .sumReservedAmounts(List.of(productId))
            .collectMap(ProductReservedAmount::getProductId, ProductReservedAmount::getReservedAmount)
        ;
        var availabilityResult = productAvailabilitiesRepo
            .findByProductId(productId)
            .defaultIfEmpty(new ProductAvailability(productId, 0, 0, null))
        ;

        return Mono
            .zip(reservedResult, productResult, availabilityResult)
            .flatMap(t3 -> {
                var reservedAmounts = t3.getT1();
                var product = t3.getT2();
                var availability = t3.getT3();

                var prodId = availability.getProductId();
                availability.setReservedAmount(reservedAmounts.getOrDefault(prodId, 0));
                availability.setStock(product.getStock());
                availability.setUpdatedAt(Instant.now());

                return productAvailabilitiesRepo.save(availability);
            })
        ;
    }

    public Mono<List<ProductAvailability>> syncAllWithReservations(List<String> productIds) {
        var reservedAmountsResult = reservationsRepo
            .sumReservedAmounts(productIds)
            .collectMap(ProductReservedAmount::getProductId, ProductReservedAmount::getReservedAmount)
        ;
        var stocksResult = productsRepo
            .findAllById(productIds)
            .collectMap(Product::getId, Product::getStock)
        ;
        var availabilitiesResult = productAvailabilitiesRepo
            .findAllByProductIds(productIds)
        ;

        return Mono
            .zip(reservedAmountsResult, stocksResult, availabilitiesResult)
            .flatMap(t3 -> {
                var reservedAmounts = t3.getT1();
                var stocks = t3.getT2();
                var availabilitiesMap = new HashMap<String, ProductAvailability>();
                for (var prodId: productIds) {
                    var found = t3.getT3().stream().filter(a -> a != null && Objects.equals(a.getProductId(), prodId)).toList();
                    if (found.isEmpty()) {
                        availabilitiesMap.put(prodId, new ProductAvailability(prodId, 0, 0, null));
                    } else {
                        availabilitiesMap.put(prodId, found.getFirst());
                    }
                }
                var availabilities = new ArrayList<ProductAvailability>();
                for (var productId: availabilitiesMap.keySet()) {
                    var availability = availabilitiesMap.get(productId);
                    availability.setReservedAmount(reservedAmounts.getOrDefault(productId, 0));
                    availability.setStock(stocks.getOrDefault(productId, 0));
                    availability.setUpdatedAt(Instant.now());
                    availabilities.add(availability);
                }
                return productAvailabilitiesRepo.saveMany(availabilities);
            })
        ;
    }
}
