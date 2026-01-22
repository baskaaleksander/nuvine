package com.baskaaleksander.nuvine.integration.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

public final class TestContainersConfig {

    private TestContainersConfig() {}

    public static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    public static final LocalStackContainer LOCALSTACK =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3)
            .withReuse(true);

    static {
        REDIS.start();
        LOCALSTACK.start();
    }
}
