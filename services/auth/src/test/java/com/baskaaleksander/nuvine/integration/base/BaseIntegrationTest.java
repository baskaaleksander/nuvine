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
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "true");

        registry.add("spring.kafka.bootstrap-servers", () -> kafkaBootstrapServers());

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.redis.redisson.config",
                () -> "singleServerConfig:\n  address: redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));

        registry.add("eureka.client.enabled", () -> "false");

        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");
    }
}
