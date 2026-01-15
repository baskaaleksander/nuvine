package com.baskaaleksander.nuvine.integration.base;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.baskaaleksander.nuvine.integration.config.TestContainersConfig.*;

/**
 * Base class for all integration tests.
 * Provides container configuration and dynamic property sources.
 */
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
        // PostgreSQL properties
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Flyway properties
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "true");

        // Kafka properties
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        // Redis/Redisson properties
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // Redisson config for workspace service
        registry.add("spring.redis.redisson.config", () -> 
            "singleServerConfig:\n  address: redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));

        // Disable Eureka for tests
        registry.add("eureka.client.enabled", () -> "false");

        // Disable Config Server for tests
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");
    }
}
