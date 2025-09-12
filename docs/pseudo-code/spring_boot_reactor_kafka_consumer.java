/*
README (quick)

This is a minimal Spring Boot + Reactor Kafka consumer example.
It uses reactor-kafka (non-blocking) to consume messages from a Kafka topic
and acknowledges offsets after processing.

Includes:
 - pom.xml snippet for Maven dependencies
 - src/main/resources/application.yml
 - Java files: Application, KafkaConfig, CartEventConsumer, CartEvent

Run instructions:
 - Start Kafka locally (bootstrap: localhost:9092)
 - Build & run this Spring Boot app
 - Produce messages to the configured topic (cart-events)

Notes:
 - This example uses Reactor Kafka (io.projectreactor.kafka:reactor-kafka).
 - Offsets are acknowledged after successful processing using ReceiverOffset.acknowledge().
 - You can tune commit batching with ReceiverOptions.commitBatchSize/commitInterval.
*/

/* ---------- pom.xml (snippet) ----------
<dependencies>
    <!-- Spring Boot WebFlux (optional, for exposing endpoints/health) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Reactor Kafka -->
    <dependency>
        <groupId>io.projectreactor.kafka</groupId>
        <artifactId>reactor-kafka</artifactId>
        <version>1.3.23</version>
    </dependency>

    <!-- Jackson for JSON mapping -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-logging</artifactId>
    </dependency>
</dependencies>
*/

/* ---------- src/main/resources/application.yml ----------
app:
  kafka:
    bootstrap-servers: localhost:9092
    topic: cart-events
    group-id: cart-consumer-group

logging:
  level:
    root: INFO
    com.example: DEBUG
*/

package com.example.kafkareactor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class KafkaReactorApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaReactorApplication.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

@Component
class KafkaConfig {
    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${app.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.group-id}")
    private String groupId;

    @Value("${app.kafka.topic}")
    private String topic;

    /**
     * Build ReceiverOptions for Reactor Kafka.
     * Tune commitInterval and commitBatchSize to control offset committing behavior.
     */
    @Bean
    public ReceiverOptions<String, String> receiverOptions() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("group.id", groupId);
        // disable auto commit - reactor-kafka expects manual-ish acknowledgement
        props.put("enable.auto.commit", "false");
        // tune if necessary
        props.put("auto.offset.reset", "earliest");

        ReceiverOptions<String, String> basicOptions = ReceiverOptions.<String, String>create(props)
            .subscription(Set.of(topic))
            // commit every 100 acked records or every 1 second
            .commitBatchSize(100)
            .commitBatchDuration(Duration.ofSeconds(1));

        log.info("Created ReceiverOptions for topic={}", topic);
        return basicOptions;
    }
}

@Component
class CartEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CartEventConsumer.class);

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObjectMapper objectMapper;
    private Disposable consumptionDisposable;

    CartEventConsumer(ReceiverOptions<String, String> receiverOptions, ObjectMapper objectMapper,
                      @Value("${app.kafka.topic}") String topic) {
        this.objectMapper = objectMapper;
        this.kafkaReceiver = KafkaReceiver.create(receiverOptions);

        // start consuming on construction (you could start on ApplicationReadyEvent instead)
        startConsuming();
    }

    private void startConsuming() {
        Flux<ReceiverRecord<String, String>> inbound = kafkaReceiver.receive();

        // process each record, acknowledge offset only after successful processing
        consumptionDisposable = inbound
            .flatMap(record -> {
                String key = record.key();
                String value = record.value();

                log.debug("Received record: partition={}, offset={}, key={}, value={}",
                    record.partition(), record.offset(), key, value);

                return processMessage(value)
                    // acknowledge after processing
                    .doOnSuccess(v -> record.receiverOffset().acknowledge())
                    // if processing fails, decide what to do: here we log and do NOT ack so it can be retried
                    .doOnError(err -> log.error("Processing failed for offset={} key={}", record.offset(), key, err))
                    // convert to empty Mono to continue the chain
                    .onErrorResume(err -> Mono.empty());
            },  // concurrency
            16) // parallelism for processing across partitions (safe because Reactor Kafka preserves per-partition ordering when using receive())
            .doOnSubscribe(s -> log.info("Kafka consumer subscribed"))
            .doOnError(err -> log.error("Kafka flux error", err))
            .subscribe();
    }

    /**
     * Example message processing: parse JSON into CartEvent and simulate writing to storage.
     * Replace this with actual reactive repository or business logic.
     */
    private Mono<Void> processMessage(String json) {
        return Mono.fromCallable(() -> objectMapper.readValue(json, CartEvent.class))
            .flatMap(event -> {
                log.info("Processing cart event for userId={} items={}", event.getUserId(), event.getItems());
                // TODO: write to DB/reactive repository. Simulate IO with delay.
                return Mono.delay(Duration.ofMillis(100)).then();
            })
            .onErrorResume(ex -> {
                // handle parse error or processing error
                log.error("Failed to parse/process message", ex);
                return Mono.error(ex);
            });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Kafka consumer");
        if (consumptionDisposable != null && !consumptionDisposable.isDisposed()) {
            consumptionDisposable.dispose();
        }
        kafkaReceiver.close();
    }
}

// Simple DTO
class CartEvent {
    private String userId;
    private java.util.List<String> items;

    public CartEvent() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public java.util.List<String> getItems() { return items; }
    public void setItems(java.util.List<String> items) { this.items = items; }

    @Override
    public String toString() {
        return "CartEvent{userId='" + userId + "', items=" + items + "}";
    }
}
