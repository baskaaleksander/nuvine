package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingCompletedEventProducer {

    private final KafkaTemplate<String, EmbeddingCompletedEvent> kafkaTemplate;

    @Value("${topics.embedding-completed-topic}")
    private String topic;

    public void sendEmbeddingCompletedEvent(EmbeddingCompletedEvent event) {
        log.info("EMBEDDING_COMPLETED_EVENT start embeddingJobId={} embeddedChunksCount={}", event.ingestionJobId(), event.embeddedChunks().size());
        Message<EmbeddingCompletedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();
        kafkaTemplate.send(message);

        log.info("EMBEDDING_COMPLETED_EVENT sent embeddingJobId={} embeddedChunksCount={}", event.ingestionJobId(), event.embeddedChunks().size());
    }
}
