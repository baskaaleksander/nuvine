package com.baskaaleksander.nuvine.integration.config;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public final class TestContainersConfig {

    private TestContainersConfig() {}

    public static final MongoDBContainer MONGODB =
        new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
            .withReuse(true);

    public static final KafkaContainer KAFKA =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"))
            .withReuse(true);

    static {
        MONGODB.start();
        KAFKA.start();
    }
}
