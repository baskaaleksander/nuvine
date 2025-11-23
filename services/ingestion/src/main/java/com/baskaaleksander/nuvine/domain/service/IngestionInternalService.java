package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import com.baskaaleksander.nuvine.application.pagination.PaginationUtil;
import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.IngestionJobSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionInternalService {

    private final IngestionJobRepository ingestionJobRepository;

    public PagedResponse<IngestionJob> getAllJobs(String workspaceId, String projectId, IngestionStatus status, PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);

        UUID workspaceUuid = workspaceId != null ? UUID.fromString(workspaceId) : null;
        UUID projectUuid = projectId != null ? UUID.fromString(projectId) : null;

        Specification<IngestionJob> spec = Specification.allOf(
                IngestionJobSpec.hasWorkspaceId(workspaceUuid),
                IngestionJobSpec.hasProjectId(projectUuid),
                IngestionJobSpec.hasStatus(status)
        );

        Page<IngestionJob> page = ingestionJobRepository.findAll(spec, pageable);


    }
}
