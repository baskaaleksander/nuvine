package com.baskaaleksander.nuvine.infrastructure.config;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedDlqMessage;
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
public class VectorProcessingCompletedDlqConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${dlq.vector-processing-completed.processing-delay-ms:300000}")
    private long processingDelayMs;

    @Value("${dlq.vector-processing-completed.batch-size:100}")
    private int batchSize;

    private ConsumerFactory<String, VectorProcessingCompletedDlqMessage> createVectorProcessingCompletedDlqConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "vector-processing-completed-dlq-worker");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, (int) processingDelayMs);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, (int) processingDelayMs + 60000);

        JsonDeserializer<VectorProcessingCompletedDlqMessage> deserializer = new JsonDeserializer<>(VectorProcessingCompletedDlqMessage.class);
        deserializer.addTrustedPackages("com.baskaaleksander.nuvine.infrastructure.messaging.dto");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VectorProcessingCompletedDlqMessage> vectorProcessingCompletedDlqKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, VectorProcessingCompletedDlqMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createVectorProcessingCompletedDlqConsumerFactory());
        factory.setBatchListener(true);
        factory.getContainerProperties().setIdleBetweenPolls(processingDelayMs);
        return factory;
    }
}
