package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedDlqMessage;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingCompletedEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingCompletedDlqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingCompletedEventConsumer {

    private final EmbeddingService service;
    private final EmbeddingCompletedDlqProducer dlqProducer;

    @Value("${topics.embedding-completed-topic}")
    private String embeddingCompletedTopic;

    @KafkaListener(topics = "${topics.embedding-completed-topic}")
    public void consumeEmbeddingCompletedEvent(EmbeddingCompletedEvent event) {
        log.info("EMBEDDING_COMPLETED_EVENT received embeddingJobId={} embeddedChunksCount={}",
                event.ingestionJobId(), event.embeddedChunks().size());
        try {
            service.processEmbeddingCompletedEvent(event);
            log.info("EMBEDDING_COMPLETED_EVENT processed embeddingJobId={} embeddedChunksCount={}",
                    event.ingestionJobId(), event.embeddedChunks().size());
        } catch (Exception e) {
            log.error("EMBEDDING_COMPLETED_EVENT failed embeddingJobId={} error={}",
                    event.ingestionJobId(), e.getMessage(), e);

            EmbeddingCompletedDlqMessage dlqMessage = EmbeddingCompletedDlqMessage.createInitial(
                    event, e, embeddingCompletedTopic);

            if (isPermanentFailure(e)) {
                log.error("EMBEDDING_COMPLETED_EVENT permanent failure - sending to dead letter embeddingJobId={}",
                        event.ingestionJobId());
                dlqProducer.sendToDeadLetter(dlqMessage);
            } else {
                dlqProducer.sendToDlq(dlqMessage);
            }
        }
    }

    private boolean isPermanentFailure(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("Job not found") ||
               message.contains("not found") ||
               e instanceof IllegalArgumentException;
    }
}
