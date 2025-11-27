package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmbeddingRequestEventProducer {

    private final KafkaTemplate<String, EmbeddingRequestEvent> kafkaTemplate;

    @Value("${topics.embedding-request-topic}")
    private String topic;
    
    public void sendEmbeddingRequestEvent(EmbeddingRequestEvent event) {
        log.info("EMBEDDING_REQUEST_EVENT START embeddingJobId={}", event.embeddingJobId());

        Message<EmbeddingRequestEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaTemplate.send(message);

        log.info("EMBEDDING_REQUEST_EVENT END embeddingJobId={}", event.embeddingJobId());
    }
}
