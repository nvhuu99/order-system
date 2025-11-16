package com.example.auth_service.services.auth;

import com.example.auth_service.services.auth.dto.AuthTokens;
import com.example.auth_service.services.auth.dto.BasicAuthRequest;
import com.example.auth_service.services.auth.dto.RefreshAccessTokenRequest;
import com.example.auth_service.services.auth.dto.VerifyAccessTokenRequest;
import reactor.core.publisher.Mono;

public interface AuthenticationService {

    Mono<AuthTokens> basicAuth(BasicAuthRequest request);

    Mono<Void> verifyAccessToken(VerifyAccessTokenRequest request);

    Mono<AuthTokens> refreshAccessToken(RefreshAccessTokenRequest request);
}
