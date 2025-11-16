package com.example.api_gateway.services.auth_service;

import com.example.api_gateway.services.auth_service.exceptions.AuthServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

public class AuthServiceClientConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return (String methodKey, Response response) -> {
            try {
                return AuthServiceException.mapFromResponse(response);
            } catch (Exception parseException) {
                return new ErrorDecoder.Default().decode(methodKey, response);
            }
        };
    }
}

