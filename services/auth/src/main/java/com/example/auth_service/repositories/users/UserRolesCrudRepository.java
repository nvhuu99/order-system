package com.example.auth_service.repositories.users;

import com.example.auth_service.repositories.users.entities.UserRole;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface UserRolesCrudRepository extends ReactiveCrudRepository<UserRole, Long> {
    Flux<UserRole> findByUsername(String username);
}
