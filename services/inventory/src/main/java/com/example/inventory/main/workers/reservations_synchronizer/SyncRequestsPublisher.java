package com.example.inventory.main.workers.reservations_synchronizer;

import com.example.inventory.repositories.products.ProductsCrudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

public class SyncRequestsPublisher {

    @Value("${order-system.messaging.product-reservation-sync-requests.topic-name}")
    private String topic;

    @Value("${order-system.messaging.product-reservation-sync-requests.batch-size}")
    private Integer batchSize;

    @Value("${order-system.messaging.product-reservation-sync-requests.request-expires-after-seconds}")
    private Integer requestExpiresAfterSeconds;

    @Autowired
    private KafkaTemplate<String, SyncRequest> kafka;

    @Autowired
    private ProductsCrudRepository productsRepo;


    public void execute() {
        var countProducts = productsRepo.count().block();
        var countBatches =  Math.toIntExact((countProducts + batchSize - 1L) / batchSize);
        if (countProducts == 0) {
            return;
        }

        var batch = 1;
        var expiresAt = Instant.now().plusSeconds(requestExpiresAfterSeconds);
        while (batch <= countBatches) {
            publishRequest(new SyncRequest(batchSize, batch, expiresAt));
            batch++;
        }
    }

    public void publishRequest(SyncRequest request) {
        String key = request.getBatchSize() + "-" + request.getBatchNumber();
        kafka.send(topic, key, request);
    }
}
