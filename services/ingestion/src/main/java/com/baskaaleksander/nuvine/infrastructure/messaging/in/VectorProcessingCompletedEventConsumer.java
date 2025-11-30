package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.IngestionStatusOrchestrator;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorProcessingCompletedEventConsumer {

    private IngestionStatusOrchestrator ingestionStatusOrchestrator;

    @KafkaListener(topics = "${topics.vector-processing-completed-topic}")
    public void consumeVectorProcessingCompletedEvent(VectorProcessingCompletedEvent event) {
        log.info("VECTOR_PROCESSING_COMPLETED_EVENT received embeddingJobId={} documentId={} projectId={} workspaceId={}", event.ingestionJobId(), event.documentId(), event.projectId(), event.workspaceId());
        ingestionStatusOrchestrator.handleVectorProcessingCompleted(event.ingestionJobId());
        log.info("VECTOR_PROCESSING_COMPLETED_EVENT processed embeddingJobId={} documentId={} projectId={} workspaceId={}", event.ingestionJobId(), event.documentId(), event.projectId(), event.workspaceId());
    }
}
