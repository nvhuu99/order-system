package com.example.inventory.services.product_availabilities;

import com.example.inventory.repositories.product_availabilities.ProductAvailabilitiesRepository;
import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import com.example.inventory.repositories.product_reservations.ProductReservationsRepository;
import com.example.inventory.repositories.product_reservations.dto.ProductReservedAmount;
import com.example.inventory.repositories.products.ProductsRepository;
import com.example.inventory.repositories.products.entities.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ProductAvailabilityServiceImp implements ProductAvailabilityService {

    @Autowired
    private ProductReservationsRepository reservationsRepo;

    @Autowired
    private ProductsRepository productsRepo;

    @Autowired
    private ProductAvailabilitiesRepository productAvailabilitiesRepo;


    public Mono<ProductAvailability> syncWithReservations(String productId) {
        var reservedResult = reservationsRepo
            .sumReservedAmountByProductIds(List.of(productId))
            .collectMap(ProductReservedAmount::getProductId, ProductReservedAmount::getReserved)
        ;
        var productResult = productsRepo.findById(productId);
        var availabilityResult = productAvailabilitiesRepo.findByProductId(productId);

        return Mono
            .zip(reservedResult, productResult, availabilityResult)
            .flatMap(t3 -> {
                var reservedAmounts = t3.getT1();
                var product = t3.getT2();
                var availability = t3.getT3();

                var prodId = availability.getProductId();
                availability.setReserved(reservedAmounts.getOrDefault(prodId, 0));
                availability.setStock(product.getStock());

                return productAvailabilitiesRepo.save(availability);
            })
        ;
    }

    public Mono<List<ProductAvailability>> syncAllWithReservations(List<String> productIds) {
        var reservedAmountsResult = reservationsRepo
            .sumReservedAmountByProductIds(productIds)
            .collectMap(ProductReservedAmount::getProductId, ProductReservedAmount::getReserved)
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
                var availabilities = t3.getT3();

                for (var availability: availabilities) {
                    var prodId = availability.getProductId();
                    availability.setReserved(reservedAmounts.getOrDefault(prodId, 0));
                    availability.setStock(stocks.getOrDefault(prodId, 0));
                }

                return productAvailabilitiesRepo.saveMany(availabilities);
            })
        ;
    }
}
