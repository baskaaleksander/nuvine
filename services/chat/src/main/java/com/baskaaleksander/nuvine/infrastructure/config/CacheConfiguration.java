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

    public static final String KEYCLOAK_TOKEN_CACHE = "keycloak-tokens";
    public static final String USER_CONVERSATIONS_CACHE = "user-conversations";
    public static final String CONVERSATION_MESSAGES_CACHE = "conversation-messages";

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

        MutableConfiguration<String, Object> rateBucketConfig = createConfig(TimeUnit.DAYS, 2);
        MutableConfiguration<String, Object> keycloakTokenConfig = createConfig(TimeUnit.MINUTES, 4);
        MutableConfiguration<String, Object> userConversationsConfig = createConfig(TimeUnit.MINUTES, 5);
        MutableConfiguration<String, Object> messagesConfig = createConfig(TimeUnit.MINUTES, 10);

        createCache(manager, redissonClient, "chat-service-buckets", rateBucketConfig);

        createCache(manager, redissonClient, KEYCLOAK_TOKEN_CACHE, keycloakTokenConfig);
        createCache(manager, redissonClient, USER_CONVERSATIONS_CACHE, userConversationsConfig);
        createCache(manager, redissonClient, CONVERSATION_MESSAGES_CACHE, messagesConfig);

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
