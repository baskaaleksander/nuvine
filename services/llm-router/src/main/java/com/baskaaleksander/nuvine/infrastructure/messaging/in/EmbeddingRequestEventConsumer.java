package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.model.EmbeddedChunk;
import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingCompletedEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingRequestEventConsumer {

    private final EmbeddingService embeddingService;
    private final EmbeddingCompletedEventProducer eventProducer;

    @KafkaListener(topics = "${topics.embedding-request-topic}")
    public void consumeEmbeddingRequestEvent(EmbeddingRequestEvent event) {
        log.info("EMBEDDING_REQUEST_EVENT received embeddingJobId={} chunksCount={}", event.embeddingJobId(), event.chunks().size());
        try {
            List<EmbeddedChunk> embeddedChunks = embeddingService.createEmbeddings(event.chunks());
            eventProducer.sendEmbeddingCompletedEvent(new EmbeddingCompletedEvent(event.embeddingJobId(), embeddedChunks, event.model()));
            log.info("EMBEDDING_REQUEST_EVENT PROCESSED embeddingJobId={} embeddedChunksCount={}", event.embeddingJobId(), embeddedChunks.size());
        } catch (Exception ex) {
            log.error("EMBEDDING_REQUEST_EVENT failed embeddingJobId={} error={}", event.embeddingJobId(), ex.getMessage(), ex);
        }
    }
}
