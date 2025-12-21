package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.IngestionJob;
import com.baskaaleksander.nuvine.domain.model.IngestionStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionJobSpecTest {

    @Mock
    private Root<IngestionJob> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
    }

    @Test
    void hasWorkspaceId_withValidId_returnsPredicateSpec() {
        UUID workspaceId = UUID.randomUUID();
        when(root.get("workspaceId")).thenReturn(path);
        when(criteriaBuilder.equal(path, workspaceId)).thenReturn(predicate);

        Specification<IngestionJob> spec = IngestionJobSpec.hasWorkspaceId(workspaceId);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(predicate, result);
        verify(root).get("workspaceId");
        verify(criteriaBuilder).equal(path, workspaceId);
    }

    @Test
    void hasWorkspaceId_withNullId_returnsNull() {
        Specification<IngestionJob> spec = IngestionJobSpec.hasWorkspaceId(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertNull(result);
        verify(root, never()).get(anyString());
    }

    @Test
    void hasProjectId_withValidId_returnsPredicateSpec() {
        UUID projectId = UUID.randomUUID();
        when(root.get("projectId")).thenReturn(path);
        when(criteriaBuilder.equal(path, projectId)).thenReturn(predicate);

        Specification<IngestionJob> spec = IngestionJobSpec.hasProjectId(projectId);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(predicate, result);
        verify(root).get("projectId");
        verify(criteriaBuilder).equal(path, projectId);
    }

    @Test
    void hasProjectId_withNullId_returnsNull() {
        Specification<IngestionJob> spec = IngestionJobSpec.hasProjectId(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertNull(result);
    }

    @Test
    void hasStatus_withValidStatus_returnsPredicateSpec() {
        IngestionStatus status = IngestionStatus.PROCESSING;
        when(root.get("status")).thenReturn(path);
        when(criteriaBuilder.equal(path, status)).thenReturn(predicate);

        Specification<IngestionJob> spec = IngestionJobSpec.hasStatus(status);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertEquals(predicate, result);
        verify(root).get("status");
        verify(criteriaBuilder).equal(path, status);
    }

    @Test
    void hasStatus_withNullStatus_returnsNull() {
        Specification<IngestionJob> spec = IngestionJobSpec.hasStatus(null);
        Predicate result = spec.toPredicate(root, query, criteriaBuilder);

        assertNull(result);
    }
}
