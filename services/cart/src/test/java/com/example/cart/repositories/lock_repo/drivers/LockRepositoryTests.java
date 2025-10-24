package com.example.cart.repositories.lock_repo.drivers;

import com.example.cart.TestBase;
import com.example.cart.repositories.lock_repo.LockRepository;
import com.example.cart.repositories.lock_repo.exceptions.LockUnavailable;
import com.example.cart.repositories.lock_repo.exceptions.LockValueMismatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LockRepositoryTests extends TestBase {

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
    void whenReleaseNotExistingLock_thenNoException() {
        var randomResourceId = UUID.randomUUID().toString();
        var releaseLock = lockRepo.releaseLock(randomResourceId, "lockValue");

        assertThatNoException().isThrownBy(releaseLock::block);
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