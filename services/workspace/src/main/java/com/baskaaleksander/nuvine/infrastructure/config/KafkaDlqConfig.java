package com.baskaaleksander.nuvine.infrastructure.config;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.DocumentIngestionDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.WorkspaceMemberDataUpdateDlqMessage;
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

    @Value("${dlq.document-ingestion.processing-delay-ms:300000}")
    private long processingDelayMs;

    @Value("${dlq.document-ingestion.batch-size:100}")
    private int batchSize;

    @Value("${dlq.workspace-member-data-update.processing-delay-ms:300000}")
    private long workspaceMemberDataUpdateProcessingDelayMs;

    @Value("${dlq.workspace-member-data-update.batch-size:100}")
    private int workspaceMemberDataUpdateBatchSize;

    @Bean
    public ConsumerFactory<String, DocumentIngestionDlqMessage> dlqConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "workspace-dlq-worker");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, (int) processingDelayMs);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, (int) processingDelayMs + 60000);

        JsonDeserializer<DocumentIngestionDlqMessage> deserializer = new JsonDeserializer<>(DocumentIngestionDlqMessage.class);
        deserializer.addTrustedPackages("com.baskaaleksander.nuvine.infrastructure.messaging.dto");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentIngestionDlqMessage> dlqKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DocumentIngestionDlqMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(dlqConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleBetweenPolls(processingDelayMs);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, WorkspaceMemberDataUpdateDlqMessage> workspaceMemberDataUpdateDlqConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "workspace-member-data-update-dlq-worker");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, workspaceMemberDataUpdateBatchSize);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, (int) workspaceMemberDataUpdateProcessingDelayMs);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, (int) workspaceMemberDataUpdateProcessingDelayMs + 60000);

        JsonDeserializer<WorkspaceMemberDataUpdateDlqMessage> deserializer =
                new JsonDeserializer<>(WorkspaceMemberDataUpdateDlqMessage.class);
        deserializer.addTrustedPackages("com.baskaaleksander.nuvine.infrastructure.messaging.dto");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkspaceMemberDataUpdateDlqMessage> workspaceMemberDataUpdateDlqKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WorkspaceMemberDataUpdateDlqMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(workspaceMemberDataUpdateDlqConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleBetweenPolls(workspaceMemberDataUpdateProcessingDelayMs);
        return factory;
    }
}
