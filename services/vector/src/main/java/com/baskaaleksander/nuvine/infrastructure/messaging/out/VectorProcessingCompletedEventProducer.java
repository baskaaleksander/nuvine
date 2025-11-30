package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
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
public class VectorProcessingCompletedEventProducer {

    private final KafkaTemplate<String, VectorProcessingCompletedEvent> kafkaTemplate;

    @Value("${topics.vector-processing-completed-topic}")
    private String topic;

    public void sendVectorProcessingCompletedEvent(VectorProcessingCompletedEvent event) {
        log.info("VECTOR_PROCESSING_COMPLETED_EVENT START embeddingJobId={}", event.ingestionJobId());

        Message<VectorProcessingCompletedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaTemplate.send(message);

        log.info("VECTOR_PROCESSING_COMPLETED_EVENT END embeddingJobId={}", event.ingestionJobId());
    }
}
