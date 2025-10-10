package com.example.cart.repositories.lock_repo.drivers;

import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.repositories.lock_repo.LockResolveType;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class LockRepositoryTests {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4.2-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.connect-timeout", () -> 5);
    }

    @Autowired
    private LockRepository lockRepo;

    @Test
    void whenAcquireAvailableLock_thenNoException() {
        var resource = UUID.randomUUID().toString();

        var acquireLock = lockRepo.acquireLock("A", resource, 10000L);

        assertThatNoException().isThrownBy(acquireLock::block);
    }

    @Test
    void whenAcquireNewLock_thenCreateNewLock() {
        var resource = UUID.randomUUID().toString();

        var resolveType = lockRepo.acquireLock("A", resource, 10000L).block();

        assertEquals(LockResolveType.CREATE_NEW, resolveType);
    }

    @Test
    void ifLockAlreadyResolvedBefore_whenAcquireThenResolveFromInMemory() {
        var resource = UUID.randomUUID().toString();

        var firstAttempt = lockRepo.acquireLock("A", resource, 10000L);
        var secAttempt = lockRepo.acquireLock("A", resource, 10000L);

        assertEquals(LockResolveType.IN_MEMORY, firstAttempt.then(secAttempt).block());
    }

    @Test
    void whenAcquireUnavailableLock_thenThrowException() {
        var resource = UUID.randomUUID().toString();

        var firstOwnerAttempt = lockRepo.acquireLock("A", resource, 10000L);
        var secondOwnerAttempt = lockRepo.acquireLock("B", resource, 10000L);

        assertThrows(LockUnavailable.class, firstOwnerAttempt.then(secondOwnerAttempt)::block);
    }
}
