package com.example.inventory.main.workers.reservations_synchronizer;

import com.example.inventory.repositories.products.ProductsCrudRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SyncRequestsScheduledPublisher {

    @Value("${order-system.messaging.product-reservation-sync-requests.topic-name}")
    private String topic;

    @Value("${order-system.messaging.product-reservation-sync-requests.batch-size}")
    private Integer batchSize;

    @Value("${order-system.workers.product-reservation-sync-requests.schedule-rate-ms}")
    private Integer scheduleRateMs;

    @Autowired
    private KafkaTemplate<String, SyncRequest> kafka;

    @Autowired
    private ProductsCrudRepository productsRepo;

    @Scheduled(fixedDelayString = "${order-system.workers.product-reservation-sync-requests.schedule-rate-ms}")
    public void execute() {
        var countProducts = productsRepo.count().block();
        if (countProducts == 0) {
            return;
        }

        var batch = 1;
        var countBatches =  Math.toIntExact((countProducts + batchSize - 1L) / batchSize);
        var expiresAt = Instant.now().plusSeconds(scheduleRateMs);
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
