package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.IngestionJobConciseResponse;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import org.springframework.stereotype.Component;

@Component
public class IngestionJobMapper {

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
