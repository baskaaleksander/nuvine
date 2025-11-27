package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.model.EmbeddingJob;
import com.baskaaleksander.nuvine.domain.model.EmbeddingStatus;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmbeddingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.VectorProcessingRequestEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.out.EmbeddingRequestEventProducer;
import com.baskaaleksander.nuvine.infrastructure.repository.EmbeddingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingRequestEventProducer embeddingRequestEventProducer;
    private final EmbeddingJobRepository jobRepository;

    public void process(VectorProcessingRequestEvent event) {
        int totalChunks = event.chunks().size();

        EmbeddingJob job = EmbeddingJob.builder()
                .ingestionJobId(UUID.fromString(event.ingestionJobId()))
                .documentId(UUID.fromString(event.documentId()))
                .projectId(UUID.fromString(event.projectId()))
                .workspaceId(UUID.fromString(event.workspaceId()))
                .totalChunks(totalChunks)
                .processedChunks(0)
                .status(EmbeddingStatus.IN_PROGRESS)
                .build();

        job = jobRepository.save(job);
        
        List<List<Chunk>> partitionedChunks = partition(event.chunks(), 10);

        for (var batch : partitionedChunks) {
            List<Integer> indexes = batch.stream()
                    .map(Chunk::index)
                    .toList();

            List<String> texts = batch.stream()
                    .map(Chunk::content)
                    .toList();

            EmbeddingRequestEvent batchEvent = new EmbeddingRequestEvent(
                    job.getId().toString(),
                    texts,
                    indexes,
                    "text-embedding-3-small"
            );

            embeddingRequestEventProducer.sendEmbeddingRequestEvent(batchEvent);
        }
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
