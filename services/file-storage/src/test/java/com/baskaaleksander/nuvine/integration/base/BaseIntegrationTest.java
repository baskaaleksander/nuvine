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
        // Redis configuration
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");

        // LocalStack S3 configuration
        String localstackEndpoint = LOCALSTACK.getEndpointOverride(
            org.testcontainers.containers.localstack.LocalStackContainer.Service.S3
        ).toString();

        registry.add("s3.internal-url", () -> localstackEndpoint);
        registry.add("s3.external-url", () -> localstackEndpoint);
        registry.add("s3.access.name", () -> LOCALSTACK.getAccessKey());
        registry.add("s3.access.secret", () -> LOCALSTACK.getSecretKey());
        registry.add("s3.bucket-name", () -> "test-bucket");
        registry.add("s3.webhook.secret", () -> "test-webhook-secret");

        // Disable Eureka and Config Server
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");
    }
}
