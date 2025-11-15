package com.example.auth_service.repositories;

import com.example.auth_service.repositories.entities.UserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface UserRolesCrudRepository extends ReactiveCrudRepository<UserRole, Long> {
    Flux<UserRole> findByUsername(String username);
}
