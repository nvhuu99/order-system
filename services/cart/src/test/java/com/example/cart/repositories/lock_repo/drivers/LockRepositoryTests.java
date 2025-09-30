package com.example.cart.repositories.lock_repo.drivers;

import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.repositories.lock_repo.exceptions.LockValueMismatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
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
        var randomResourceId = UUID.randomUUID().toString();
        var acquireLock = lockRepo.acquireLock(randomResourceId, randomResourceId, Duration.ofSeconds(10));

        assertThatNoException().isThrownBy(acquireLock::block);
    }

    @Test
    void whenAcquireUnAvailableLock_thenThrowException() {
        var randomResourceId = UUID.randomUUID().toString();
        var acquireLock1 = lockRepo.acquireLock(randomResourceId, randomResourceId, Duration.ofSeconds(10));
        var acquireLock2 = lockRepo.acquireLock(randomResourceId, randomResourceId, Duration.ofSeconds(10));

        assertThrows(LockUnavailable.class, () -> acquireLock1.then(acquireLock2).block());
    }

    @Test
    void whenReleaseLockWithCorrectValue_thenNoException() {
        var randomResourceId = UUID.randomUUID().toString();
        var acquireLock = lockRepo.acquireLock(randomResourceId, randomResourceId, Duration.ofSeconds(10));
        var releaseLock = lockRepo.releaseLock(randomResourceId, randomResourceId);

        assertThatNoException().isThrownBy(() -> acquireLock.then(releaseLock).block());
    }

    @Test
    void whenLockValueMismatch_thenThrowException() {
        var randomResourceId = UUID.randomUUID().toString();
        var acquireLock = lockRepo.acquireLock(randomResourceId, randomResourceId, Duration.ofSeconds(10));
        var releaseLock = lockRepo.releaseLock(randomResourceId, "wrongValue");

        assertThrows(LockValueMismatch.class, () -> acquireLock.then(releaseLock).block());
    }

    @Test
    void afterLockExpires_thenAcquireLockSuccess() {
        var randomResourceId = UUID.randomUUID().toString();
        var acquireLock1 = lockRepo.acquireLock(randomResourceId, randomResourceId, Duration.ofMillis(200));
        var acquireLock2 = lockRepo.acquireLock(randomResourceId, randomResourceId, Duration.ofSeconds(10));

        assertThatNoException().isThrownBy(
            () -> acquireLock1.then(acquireLock2.retryWhen(Retry.fixedDelay(5, Duration.ofMillis(300)))).block()
        );
    }
}
