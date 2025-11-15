package com.example.auth_service.repositories;

import com.example.auth_service.repositories.entities.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsersCrudRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUsername(String username);
    Mono<User> findByAccessToken(String accessToken);
    Mono<User> findByAccessTokenAndRefreshToken(String accessToken, String refreshToken);
    Mono<Boolean> existsByUsername(String username);
    Mono<Boolean> existsByEmail(String email);
    Mono<Boolean> existsByAccessToken(String accessToken);
}
