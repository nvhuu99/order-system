package com.example.api_gateway.services.auth_service;

import com.example.api_gateway.services.auth_service.dto.BasicAuthRequest;
import com.example.api_gateway.services.auth_service.dto.RefreshAccessTokenRequest;
import com.example.api_gateway.services.auth_service.dto.RegisterRequest;
import com.example.api_gateway.services.auth_service.dto.VerifyAccessTokenRequest;
import com.example.api_gateway.services.auth_service.responses.BasicAuthResponse;
import com.example.api_gateway.services.auth_service.responses.RefreshAccessTokenResponse;
import com.example.api_gateway.services.auth_service.responses.RegisterResponse;
import com.example.api_gateway.services.auth_service.responses.VerifyAccessTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "authClient",
    url = "${auth-service.url}",
    configuration = AuthServiceClientConfig.class
)
public interface AuthServiceClient {

    @PostMapping("/api/v1/auth/register")
    RegisterResponse register(@RequestBody RegisterRequest body);

    @PostMapping("/api/v1/auth/basic-auth")
    BasicAuthResponse basicAuth(@RequestBody BasicAuthRequest body);

    @PostMapping("/api/v1/auth/verify")
    VerifyAccessTokenResponse verify(@RequestBody VerifyAccessTokenRequest body);

    @PostMapping("/api/v1/auth/refresh")
    RefreshAccessTokenResponse refresh(@RequestBody RefreshAccessTokenRequest body);
}
