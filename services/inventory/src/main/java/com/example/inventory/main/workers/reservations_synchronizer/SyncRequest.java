package com.example.inventory.main.workers.reservations_synchronizer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyncRequest {
    String requestId;
    Integer batchSize;
    Integer batchNumber;
    Instant expiresAt;
}
