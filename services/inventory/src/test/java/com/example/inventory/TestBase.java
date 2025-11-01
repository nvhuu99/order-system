package com.example.inventory;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Objects;

@SpringBootTest
@ActiveProfiles("test")
public class TestBase {

    static GenericContainer<?> redis;
    static GenericContainer<?> mysql;
    static KafkaContainer kafka;

    static {
        var withAllContainers = System.getProperty("withAllContainers", "no");
        var withRedis = System.getProperty("withRedis", "no");
        var withKafka = System.getProperty("withKafka", "no");
        var withMySQL = System.getProperty("withMySQL", "no");

        if (Objects.equals(withRedis, "yes") || Objects.equals(withAllContainers, "yes")) {
            redis = new GenericContainer<>("redis:7.4.2-alpine");
            redis.withExposedPorts(6379);
            redis.withReuse(true);
            redis.start();

            System.setProperty("spring.data.redis.host", redis.getHost());
            System.setProperty("spring.data.redis.port", String.valueOf(redis.getMappedPort(6379)));
            System.setProperty("spring.data.redis.connect-timeout", "5");
        }

        if (Objects.equals(withKafka, "yes") || Objects.equals(withAllContainers, "yes")) {
            kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.4"));
            kafka.withReuse(true);
            kafka.start();

            System.setProperty("spring.kafka.admin.auto-create", "true");
            System.setProperty("spring.kafka.listener.auto-startup", "true");
            System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());
        }

        if (Objects.equals(withMySQL, "yes") || Objects.equals(withAllContainers, "yes")) {
            mysql = new GenericContainer<>("bitnami/mysql:8.1.0-slim");
            mysql.withExposedPorts(3306);
            mysql.withReuse(true);
            mysql.withEnv(Map.of("MYSQL_ROOT_PASSWORD", "admin"));
            mysql.start();

            System.setProperty("spring.kafka.admin.auto-create", "true");
            System.setProperty("spring.kafka.listener.auto-startup", "true");
            System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());
        }
    }
}
