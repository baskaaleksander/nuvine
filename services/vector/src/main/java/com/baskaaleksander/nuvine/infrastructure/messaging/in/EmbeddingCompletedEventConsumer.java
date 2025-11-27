package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingCompletedEventConsumer {
    private final EmbeddingService service;

    @KafkaListener(topics = "${topics.embedding-completed-topic}")
    public void consumeEmbeddingCompletedEvent(EmbeddingCompletedEvent event) {
        log.info("EMBEDDING_COMPLETED_EVENT received embeddingJobId={} embeddedChunksCount={}", event.ingestionJobId(), event.embeddedChunks().size());
        service.processEmbeddingCompletedEvent(event);
        log.info("EMBEDDING_COMPLETED EVENT processed embeddingJobId={} embeddedChunksCount={}", event.ingestionJobId(), event.embeddedChunks().size());
    }
}
