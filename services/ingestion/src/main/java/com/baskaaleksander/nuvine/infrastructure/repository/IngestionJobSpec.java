package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class IngestionJobSpec {

    public static Specification<IngestionJob> hasWorkspaceId(UUID workspaceId) {
        return (root, query, cb) ->
                workspaceId == null ? null : cb.equal(root.get("workspaceId"), workspaceId);
    }

    public static Specification<IngestionJob> hasProjectId(UUID projectId) {
        return (root, query, cb) ->
                projectId == null ? null : cb.equal(root.get("projectId"), projectId);
    }

    public static Specification<IngestionJob> hasStatus(IngestionStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }
}
