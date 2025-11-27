package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingRequestEventConsumer {

    @KafkaListener(topics = "${topics.embedding-request-topic}")
    public void consumeEmbeddingRequestEvent(EmbeddingRequestEvent event) {
        log.info("EMBEDDING_REQUEST_EVENT received embeddingJobId={}", event.embeddingJobId());
        System.out.println(event);
    }
}
