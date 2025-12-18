package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.IngestionStatusOrchestrator;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.VectorProcessingCompletedDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorProcessingCompletedEventConsumer {

    private final IngestionStatusOrchestrator ingestionStatusOrchestrator;
    private final VectorProcessingCompletedDlqProducer vectorProcessingCompletedDlqProducer;

    @Value("${topics.vector-processing-completed-topic}")
    private String vectorProcessingCompletedTopic;

    @KafkaListener(topics = "${topics.vector-processing-completed-topic}")
    public void consumeVectorProcessingCompletedEvent(VectorProcessingCompletedEvent event) {
        log.info("VECTOR_PROCESSING_COMPLETED_EVENT received embeddingJobId={} documentId={} projectId={} workspaceId={}", event.ingestionJobId(), event.documentId(), event.projectId(), event.workspaceId());
        try {
            ingestionStatusOrchestrator.handleVectorProcessingCompleted(event.ingestionJobId());
            log.info("VECTOR_PROCESSING_COMPLETED_EVENT processed embeddingJobId={} documentId={} projectId={} workspaceId={}", event.ingestionJobId(), event.documentId(), event.projectId(), event.workspaceId());
        } catch (Exception e) {
            log.error("VECTOR_PROCESSING_COMPLETED_EVENT failed embeddingJobId={} documentId={} projectId={} workspaceId={} error={}",
                    event.ingestionJobId(), event.documentId(), event.projectId(), event.workspaceId(), e.getMessage(), e);
            VectorProcessingCompletedDlqMessage dlqMessage = VectorProcessingCompletedDlqMessage.createInitial(event, e, vectorProcessingCompletedTopic);
            vectorProcessingCompletedDlqProducer.sendToDlq(dlqMessage);
        }
    }
}
