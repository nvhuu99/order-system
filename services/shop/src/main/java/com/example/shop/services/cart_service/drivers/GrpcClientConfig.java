package com.example.shop.services.cart_service.drivers;

import com.example.grpc.cart.stubs.CartServiceGrpc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    public CartServiceGrpc.CartServiceStub stub(GrpcChannelFactory channelFactory) {
        var channel = channelFactory.createChannel("cart-service");
        return CartServiceGrpc.newStub(channel);
    }
}

