package com.baskaaleksander.nuvine.infrastructure.config;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceDeletedDlqMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaDlqConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${dlq.usage-logs.processing-delay-ms:300000}")
    private long processingDelayMs;

    @Value("${dlq.usage-logs.batch-size:100}")
    private int batchSize;

    @Value("${dlq.workspace-deleted.processing-delay-ms:300000}")
    private long workspaceDeletedProcessingDelayMs;

    @Value("${dlq.workspace-deleted.batch-size:100}")
    private int workspaceDeletedBatchSize;

    @Bean
    public ConsumerFactory<String, DlqMessage> dlqConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "subscription-dlq-worker");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, (int) processingDelayMs);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, (int) processingDelayMs + 60000);

        JsonDeserializer<DlqMessage> deserializer = new JsonDeserializer<>(DlqMessage.class);
        deserializer.addTrustedPackages("com.baskaaleksander.nuvine.infrastructure.messaging.dto");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DlqMessage> dlqKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DlqMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(dlqConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleBetweenPolls(processingDelayMs);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, WorkspaceDeletedDlqMessage> workspaceDeletedDlqConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "subscription-workspace-deleted-dlq-worker");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, workspaceDeletedBatchSize);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, (int) workspaceDeletedProcessingDelayMs);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, (int) workspaceDeletedProcessingDelayMs + 60000);

        JsonDeserializer<WorkspaceDeletedDlqMessage> deserializer = new JsonDeserializer<>(WorkspaceDeletedDlqMessage.class);
        deserializer.addTrustedPackages("com.baskaaleksander.nuvine.infrastructure.messaging.dto");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkspaceDeletedDlqMessage> workspaceDeletedDlqKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WorkspaceDeletedDlqMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(workspaceDeletedDlqConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleBetweenPolls(workspaceDeletedProcessingDelayMs);
        return factory;
    }
}
