package com.example.shop_app.api.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;
import com.example.grpc.inventory.stubs.ProductServiceGrpc;

@Configuration
public class GrpcClientConfig {

    @Bean
    public ProductServiceGrpc.ProductServiceBlockingStub productServiceStub(
        GrpcChannelFactory channelFactory
    ) {
        var channel = channelFactory.createChannel("product-service");
        return ProductServiceGrpc.newBlockingStub(channel);
    }
}

