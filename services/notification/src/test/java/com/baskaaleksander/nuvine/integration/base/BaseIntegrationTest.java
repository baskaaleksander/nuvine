package com.baskaaleksander.nuvine.integration.base;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.baskaaleksander.nuvine.integration.config.TestContainersConfig.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integrationtest")
@Testcontainers
public abstract class BaseIntegrationTest {

    @BeforeAll
    static void startContainers() {
        // Containers are started in TestContainersConfig static block
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MongoDB properties
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);

        // Kafka properties
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        // Disable eureka and cloud config
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");
    }
}
