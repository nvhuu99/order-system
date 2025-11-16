package com.example.api_gateway.main.gateway.configs;

import com.example.api_gateway.main.gateway.filters.TokenAuthorizationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;

@Configuration
public class RouteConfig {

    @Value("${AUTH_SVC_HOST}:${AUTH_SVC_API_PORT}")
    private String authServiceAddr;

    @Value("${INVENTORY_SVC_HOST}:${INVENTORY_SVC_API_PORT}")
    private String inventoryServiceAddr;

    @Value("${SHOP_SVC_HOST}:${SHOP_SVC_API_PORT}")
    private String shopServiceAddr;
    
    @Autowired
    TokenAuthorizationFilter tokenAuthorizationFilter;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        var routes = builder.routes();

        routes.route("api-auth", r -> r
            .path("/api/v1/auth/**")
            .uri(authServiceAddr)
        );

        routes.route("api-admin-products", r -> r
            .path("/api/v1/admin/products/**")
            .filters(f -> f.filter(tokenAuthorizationFilter))
            .uri(inventoryServiceAddr)
        );

        routes.route("api-shop", r -> r
            .path("/api/v1/carts/**")
            .filters(f -> f.filter(tokenAuthorizationFilter))
            .uri(shopServiceAddr)
        );

        return routes.build();
    }
}
