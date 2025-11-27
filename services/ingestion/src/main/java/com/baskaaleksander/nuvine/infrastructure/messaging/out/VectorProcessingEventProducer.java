package com.baskaaleksander.nuvine.infrastructure.messaging.out;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
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
public class VectorProcessingEventProducer {

    @Value("${topics.vector-processing-request-topic}")
    private String topic;

    private final KafkaTemplate<String, VectorProcessingRequestEvent> kafkaTemplate;

    public void sendVectorProcessingRequestEvent(VectorProcessingRequestEvent event) {
        log.info("VECTOR_PROCESSING_REQUEST_EVENT START ingestionJobId={}", event.ingestionJobId());

        Message<VectorProcessingRequestEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        kafkaTemplate.send(message);

    }
}