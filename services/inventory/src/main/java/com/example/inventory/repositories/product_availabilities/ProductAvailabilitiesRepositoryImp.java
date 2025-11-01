package com.example.inventory.repositories.product_availabilities;

import com.example.inventory.repositories.product_availabilities.entities.ProductAvailability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

@Service
public class ProductAvailabilitiesRepositoryImp implements ProductAvailabilitiesRepository {

    private final String keyPrefix = "order-system:product_availabilities:";

    @Autowired
    private ReactiveRedisTemplate<String, ProductAvailability> redisTemplate;


    @Override
    public Mono<ProductAvailability> findByProductId(String productId) {
        return redisTemplate.opsForValue().get(keyPrefix + productId);
    }

    @Override
    public Mono<List<ProductAvailability>> findAllByProductIds(List<String> productIds) {
        return redisTemplate
            .opsForValue()
            .multiGet(productIds.stream().map(id -> keyPrefix + id).toList())
        ;
    }

    @Override
    public Mono<ProductAvailability> save(ProductAvailability productAvailability) {
        return redisTemplate.opsForValue()
            .set(keyPrefix + productAvailability.getProductId(), productAvailability)
            .map(ok -> productAvailability)
        ;
    }

    @Override
    public Mono<List<ProductAvailability>> saveMany(List<ProductAvailability> productAvailabilities) {
        var map = new HashMap<String, ProductAvailability>();
        for (var p: productAvailabilities) {
            map.put(keyPrefix + p.getProductId(), p);
        }
        return redisTemplate
            .opsForValue()
            .multiSet(map)
            .map(ok -> productAvailabilities)
        ;
    }
}
