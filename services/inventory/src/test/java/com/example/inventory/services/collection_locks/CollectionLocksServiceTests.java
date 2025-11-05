package com.example.inventory.services.collection_locks;

import com.example.inventory.TestBase;
import com.example.inventory.services.collection_locks.exceptions.LockValueMismatch;
import com.example.inventory.services.collection_locks.exceptions.LocksUnavailable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CollectionLocksServiceTests extends TestBase {

    @Autowired
    private CollectionLocksService locksService;

    @Test
    void whenAcquireAvailableLocks_thenNoException() {
        var ids = List.of("r1", "r2", "r3");
        var acquireLock = locksService.tryLock(UUID.randomUUID().toString(), ids, "lockValue", Duration.ofSeconds(10));

        assertThatNoException().isThrownBy(acquireLock::block);
    }

    @Test
    void whenAcquireUnAvailableLock_thenThrowException() {
        var collection = UUID.randomUUID().toString();
        var acquireLock1 = locksService.tryLock(collection, List.of("r1", "r2"), "lockValue1", Duration.ofSeconds(10));
        var acquireLock2 = locksService.tryLock(collection, List.of("r1", "r2", "r3"), "lockValue2", Duration.ofSeconds(10));

        assertThrows(LocksUnavailable.class, () -> acquireLock1.then(acquireLock2).block());
    }

    @Test
    void whenReleaseLockWithCorrectValue_thenNoException() {
        var collection = UUID.randomUUID().toString();
        var acquireLock = locksService.tryLock(collection, List.of("r1", "r2"), "lockValue", Duration.ofSeconds(10));
        var releaseLock = locksService.unlock(collection, List.of("r1", "r2"), "lockValue");

        assertThatNoException().isThrownBy(() -> acquireLock.then(releaseLock).block());
    }


    @Test
    void whenLockValueMismatch_thenThrowException() {
        var collection = UUID.randomUUID().toString();
        var acquireLock = locksService.tryLock(collection, List.of("r1", "r2"), "lockValue", Duration.ofSeconds(10));
        var releaseLock = locksService.unlock(collection, List.of("r1", "r2"), "wrongValue");

        assertThrows(LockValueMismatch.class, () -> acquireLock.then(releaseLock).block());
    }

    @Test
    void afterLockExpires_thenAcquireLockSuccess() {
        var collection = UUID.randomUUID().toString();
        var acquireLock1 = locksService.tryLock(collection, List.of("r1", "r2"), "lockValue1", Duration.ofMillis(200));
        var acquireLock2 = locksService.tryLock(collection, List.of("r1", "r2", "r3"), "lockValue2", Duration.ofSeconds(10));

        assertThatNoException().isThrownBy(() ->
            acquireLock1.then(acquireLock2.retryWhen(Retry.fixedDelay(5, Duration.ofMillis(300)))).block()
        );
    }
}