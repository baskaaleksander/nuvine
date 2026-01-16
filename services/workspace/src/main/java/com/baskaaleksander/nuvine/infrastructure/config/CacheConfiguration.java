package com.baskaaleksander.nuvine.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@Profile("!integrationtest")
public class CacheConfiguration {

    public static final String KEYCLOAK_TOKEN_CACHE = "keycloak-tokens";

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    @Primary
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setPassword(redisPassword);

        config.setCodec(new Kryo5Codec());
        return Redisson.create(config);
    }

    @Bean
    @Primary
    public CacheManager jCacheCacheManager(RedissonClient redissonClient) {
        CachingProvider cachingProvider = Caching.getCachingProvider("org.redisson.jcache.JCachingProvider");
        CacheManager manager = cachingProvider.getCacheManager();

        MutableConfiguration<String, Object> rateBucketConfig = createConfig(TimeUnit.HOURS, 2);
        MutableConfiguration<String, Object> accessConfig = createConfig(TimeUnit.MINUTES, 15);
        MutableConfiguration<String, Object> keycloakTokenConfig = createConfig(TimeUnit.MINUTES, 4);

        createCache(manager, redissonClient, "workspace-service-buckets", rateBucketConfig);

        createCache(manager, redissonClient, "access-workspace-view", accessConfig);
        createCache(manager, redissonClient, "access-workspace-edit", accessConfig);
        createCache(manager, redissonClient, "access-project-manage", accessConfig);
        createCache(manager, redissonClient, "access-project-view", accessConfig);
        createCache(manager, redissonClient, "access-document-view", accessConfig);

        createCache(manager, redissonClient, "entity-workspace", accessConfig);
        createCache(manager, redissonClient, "entity-workspace-subscription", accessConfig);
        createCache(manager, redissonClient, "entity-workspace-member", accessConfig);
        createCache(manager, redissonClient, "entity-project", accessConfig);
        createCache(manager, redissonClient, "entity-document", accessConfig);
        createCache(manager, redissonClient, "entity-document-internal", accessConfig);

        createCache(manager, redissonClient, KEYCLOAK_TOKEN_CACHE, keycloakTokenConfig);

        return manager;
    }

    private MutableConfiguration<String, Object> createConfig(
            TimeUnit timeUnit,
            long timeDuration
    ) {
        MutableConfiguration<String, Object> configuration = new MutableConfiguration<>();
        configuration.setStoreByValue(false)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(
                        new Duration(timeUnit, timeDuration)))
                .setStatisticsEnabled(true);

        return configuration;
    }

    private void createCache(CacheManager manager, RedissonClient redissonClient,
                             String cacheName, MutableConfiguration<String, Object> config) {
        if (manager.getCache(cacheName) != null) {
            manager.destroyCache(cacheName);
        }

        manager.createCache(cacheName,
                RedissonConfiguration.fromInstance(redissonClient, config));
    }
}
