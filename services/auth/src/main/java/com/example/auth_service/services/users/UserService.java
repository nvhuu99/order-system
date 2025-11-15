package com.example.auth_service.services.users;

import com.example.auth_service.services.users.dto.SaveUser;
import com.example.auth_service.services.users.dto.UserDetails;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<UserDetails> findByUsername(String username);

    Mono<UserDetails> findByAccessToken(String accessToken);

    Mono<UserDetails> findByAccessTokenAndRefreshToken(String accessToken, String refreshToken);

    Mono<UserDetails> save(SaveUser data);

    Mono<Void> saveTokens(String username, String accessToken, String refreshToken);
}
