package com.example.cart.configs;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConfigs {

//    private static final Logger log = LoggerFactory.getLogger(KafkaConfigs.class);
//
//    @Value("${order-processing-system.messaging.cart-update-requests.topic-name}")
//    private String cartUpdateRequestsTopic;
//
//    @Value("${order-processing-system.messaging.cart-update-requests.topic-partitions}")
//    private List<Integer> cartUpdateRequestsTopicPartitions;
//
//    @Value("${KAFKA_REPLICAS_COUNT}")
//    private Integer kafkaReplicasCount;
//
//    @Bean
//    public KafkaAdmin admin() {
//        Map<String, Object> configs = new HashMap<>();
//        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        return new KafkaAdmin(configs);
//    }
//
//    @Bean
//    public NewTopic cartUpdateRequestsTopic() {
//        log.info("Topic - partitions: {} - replicas: {}", cartUpdateRequestsTopicPartitions, kafkaReplicasCount);
//        return TopicBuilder.name(cartUpdateRequestsTopic)
//            .partitions(cartUpdateRequestsTopicPartitions.size())
//            .replicas(kafkaReplicasCount)
//            .compact()
//            .build();
//    }
}
