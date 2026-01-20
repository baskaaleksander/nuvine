package com.baskaaleksander.nuvine.integration.base;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final int EMBEDDING_DIMENSIONS = 1536;

    @Autowired
    private QdrantClient qdrantClient;

    @Autowired
    private com.baskaaleksander.nuvine.infrastructure.config.QdrantConfig.QdrantProperties qdrantProperties;

    @BeforeAll
    static void startContainers() {
        // Containers are started in TestContainersConfig static block
    }

    @BeforeEach
    void ensureQdrantCollectionExists() {
        String collectionName = qdrantProperties.collection();
        try {
            qdrantClient.getCollectionInfoAsync(collectionName).get();
        } catch (Exception ex) {
            try {
                qdrantClient.createCollectionAsync(
                        collectionName,
                        Collections.VectorParams.newBuilder()
                                .setSize(EMBEDDING_DIMENSIONS)
                                .setDistance(Collections.Distance.Cosine)
                                .build()
                ).get();
            } catch (Exception createEx) {
                throw new IllegalStateException("Failed to create Qdrant collection " + collectionName, createEx);
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "true");

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.redis.redisson.config", () ->
            "singleServerConfig:\n  address: redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));

        registry.add("qdrant.host", QDRANT::getHost);
        registry.add("qdrant.port", () -> QDRANT.getMappedPort(6334));
        registry.add("qdrant.collection", () -> "test-collection");

        registry.add("eureka.client.enabled", () -> "false");

        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");
    }
}
