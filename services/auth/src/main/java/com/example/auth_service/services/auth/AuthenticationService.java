package com.example.auth_service.services.auth;

import com.example.auth_service.services.auth.dto.AuthTokens;
import com.example.auth_service.services.auth.dto.BasicAuthRequest;
import reactor.core.publisher.Mono;

public interface AuthenticationService {

    Mono<AuthTokens> basicAuth(BasicAuthRequest request);

    Mono<Void> verifyAccessToken(String accessToken);

    Mono<AuthTokens> refreshAccessToken(String accessToken, String refreshToken);
}
