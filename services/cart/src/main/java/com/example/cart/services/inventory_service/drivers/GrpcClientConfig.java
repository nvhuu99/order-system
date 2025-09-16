package com.example.cart.services.inventory_service.drivers;

import com.example.grpc.inventory.stubs.InventoryServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfig {

    @Bean
    public InventoryServiceGrpc.InventoryServiceStub stub(GrpcChannelFactory channelFactory) {
        var channel = channelFactory.createChannel("inventory-service");
        return InventoryServiceGrpc.newStub(channel);
    }
}

