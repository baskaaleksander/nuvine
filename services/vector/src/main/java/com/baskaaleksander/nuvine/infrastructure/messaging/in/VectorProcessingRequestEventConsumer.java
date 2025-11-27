package com.baskaaleksander.nuvine.infrastructure.messaging.in;

import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorProcessingRequestEventConsumer {

    @KafkaListener(topics = "${topics.vector-processing-request-topic}")
    public void consumeVectorProcessingRequestEvent(VectorProcessingRequestEvent event) {
        log.info("VECTOR_PROCESSING_REQUEST_EVENT received ingestionJobId={}", event.ingestionJobId());
    }
}
