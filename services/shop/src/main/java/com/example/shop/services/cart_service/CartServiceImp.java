package com.example.shop.services.cart_service;

import com.example.shop.repositories.products.ProductsRepository;
import com.example.shop.services.cart_service.entities.Cart;
import com.example.shop.services.cart_service.entities.CartItem;
import com.example.shop.services.cart_service.dto.CartUpdateRequest;
import com.example.shop.services.inventory_client.InventoryClient;
import com.example.shop.services.inventory_client.entities.ProductReservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CartServiceImp implements CartService {

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private ProductsRepository productsRepo;

    @Autowired
    private CartUpdateRequestsPublisher cartUpdateRequestsPublisher;

    @Override
    public Mono<Cart> getCartByUserId(String userId) {
        return inventoryClient
            .listProductReservations(userId)
            .collectList()
            .flatMap(reservations -> {
                var productIds = reservations.stream().map(ProductReservation::getProductId).toList();
                return productsRepo
                    .findAllById(productIds)
                    .map(product -> {
                        var reservation = reservations.stream().filter(r -> r.getProductId() == product.getId()).toList().getFirst();
                        return new CartItem(reservation, product);
                    })
                    .collectList()
                ;
            })
            .flatMap(cartItemLists -> Mono.just(new Cart(userId, cartItemLists)))
        ;
    }

    @Override
    public Mono<Void> cartUpdateRequest(CartUpdateRequest request) {
        return cartUpdateRequestsPublisher.publishRequest(request);
    }
}
