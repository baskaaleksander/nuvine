package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.IngestionJobConciseResponse;
import com.baskaaleksander.nuvine.application.dto.IngestionJobResponse;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import org.springframework.stereotype.Component;

@Component
public class IngestionJobMapper {

    public IngestionJobResponse toResponse(IngestionJob job) {
        return new IngestionJobResponse(
                job.getId(),
                job.getDocumentId(),
                job.getWorkspaceId(),
                job.getProjectId(),
                job.getStatus(),
                job.getStage(),
                job.getRetryCount(),
                job.getLastError(),
                job.getUpdatedAt(),
                job.getCreatedAt()
        );
    }

    public IngestionJobConciseResponse toConciseResponse(IngestionJob job) {
        return new IngestionJobConciseResponse(
                job.getDocumentId(),
                job.getStatus(),
                job.getStage(),
                job.getLastError(),
                job.getUpdatedAt()
        );
    }
}
