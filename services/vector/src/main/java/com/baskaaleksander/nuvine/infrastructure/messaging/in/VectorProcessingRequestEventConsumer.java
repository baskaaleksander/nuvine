package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingRequestDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorProcessingRequestEventConsumer {

    private final EmbeddingService embeddingService;
    private final VectorProcessingRequestDlqProducer vectorProcessingRequestDlqProducer;

    @Value("${topics.vector-processing-request-topic}")
    private String vectorProcessingRequestTopic;

    @KafkaListener(topics = "${topics.vector-processing-request-topic}")
    public void consumeVectorProcessingRequestEvent(VectorProcessingRequestEvent event) {
        log.info("VECTOR_PROCESSING_REQUEST_EVENT received ingestionJobId={}", event.ingestionJobId());

        try {
            embeddingService.process(event);
            log.info("VECTOR_PROCESSING_REQUEST_EVENT processed ingestionJobId={}", event.ingestionJobId());
        } catch (Exception e) {
            log.error("VECTOR_PROCESSING_REQUEST_EVENT failed ingestionJobId={} error={}",
                    event.ingestionJobId(), e.getMessage(), e);

            VectorProcessingRequestDlqMessage dlqMessage = VectorProcessingRequestDlqMessage.createInitial(event, e, vectorProcessingRequestTopic);
            vectorProcessingRequestDlqProducer.sendToDlq(dlqMessage);

            log.info("VECTOR_PROCESSING_REQUEST_EVENT sent to DLQ ingestionJobId={}", event.ingestionJobId());
        }
    }
}
