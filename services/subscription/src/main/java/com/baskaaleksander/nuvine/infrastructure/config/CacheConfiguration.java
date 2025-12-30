package com.baskaaleksander.nuvine.infrastructure.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setPassword(redisPassword);
        return Redisson.create(config);
    }

    @Bean
    @Primary
    public CacheManager jCacheCacheManager(RedissonClient redissonClient) {
        CachingProvider cachingProvider = Caching.getCachingProvider("org.redisson.jcache.JCachingProvider");
        CacheManager manager = cachingProvider.getCacheManager();

        MutableConfiguration<String, Object> configuration = createConfig(TimeUnit.HOURS, 1);
        MutableConfiguration<String, Object> modelPricingConfig = createConfig(TimeUnit.MINUTES, 30);
        MutableConfiguration<String, Object> subscriptionConfig = createConfig(TimeUnit.SECONDS, 30);
        MutableConfiguration<String, Object> externalServiceConfig = createConfig(TimeUnit.MINUTES, 5);
        MutableConfiguration<String, Object> keycloakTokenConfig = createConfig(TimeUnit.MINUTES, 4);

        createCache(manager, redissonClient, "subscription-service-buckets", configuration);
        createCache(manager, redissonClient, "model-pricing", modelPricingConfig);
        createCache(manager, redissonClient, "all-active-models", modelPricingConfig);
        createCache(manager, redissonClient, "plans", configuration);
        createCache(manager, redissonClient, "subscriptions", subscriptionConfig);
        createCache(manager, redissonClient, "users", externalServiceConfig);
        createCache(manager, redissonClient, "workspaces", externalServiceConfig);
        createCache(manager, redissonClient, "keycloak-tokens", keycloakTokenConfig);

        return manager;
    }

    private MutableConfiguration<String, Object> createConfig(
            TimeUnit timeUnit,
            long timeDuration
    ) {
        MutableConfiguration<String, Object> config = new MutableConfiguration<>();
        config.setStoreByValue(false)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(
                        new Duration(timeUnit, timeDuration)))
                .setStatisticsEnabled(true);

        return config;
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