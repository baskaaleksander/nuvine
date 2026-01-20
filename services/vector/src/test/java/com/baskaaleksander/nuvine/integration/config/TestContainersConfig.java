package com.baskaaleksander.nuvine.integration.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class TestContainersConfig {

    private TestContainersConfig() {}

    public static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    public static final KafkaContainer KAFKA =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))
            .withReuse(true);

    public static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    public static final GenericContainer<?> QDRANT =
        new GenericContainer<>(DockerImageName.parse("qdrant/qdrant:v1.16.1"))
            .withExposedPorts(6333, 6334)
            .withReuse(true);

    static {
        POSTGRES.start();
        KAFKA.start();
        REDIS.start();
        QDRANT.start();
    }
}
