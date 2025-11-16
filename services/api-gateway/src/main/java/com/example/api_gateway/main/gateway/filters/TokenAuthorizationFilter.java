package com.example.api_gateway.main.gateway.filters;

import com.example.api_gateway.services.auth_service.AuthServiceClient;
import com.example.api_gateway.services.auth_service.dto.VerifyAccessTokenRequest;
import com.example.api_gateway.services.auth_service.exceptions.AuthServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TokenAuthorizationFilter implements GatewayFilter {

    private static final Logger log = LoggerFactory.getLogger(TokenAuthorizationFilter.class);

    @Lazy
    @Autowired
    private AuthServiceClient authServiceApi;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var accessToken = extractAccessToken(exchange);
        var path = extractRequestURIPath(exchange);
        if (accessToken.isEmpty()) {
            return sendResponse(exchange, 401, null);
        }
        try {
            authServiceApi.verify(new VerifyAccessTokenRequest(accessToken, path));
            return chain.filter(exchange);
        } catch (AuthServiceException ex) {
            return sendResponse(exchange, ex.getResponse().getStatus(), ex.getResponse());
        } catch (Exception e) {
            log.error("unhandled error: {}", e.getMessage());
            return sendResponse(exchange, 500, null);
        }
    }

    private String extractRequestURIPath(ServerWebExchange exchange) {
        var path = exchange.getRequest().getURI().getPath();
        return path.isEmpty() ? "/" : path;
    }

    private String extractAccessToken(ServerWebExchange exchange) {
        var request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return "";
    }

    private Mono<Void> sendResponse(ServerWebExchange exchange, Integer status, Object body) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            var bytes = (new ObjectMapper()).writeValueAsBytes(body);
            var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception ignored) {
        }
        return exchange.getResponse().setComplete();
    }
}
