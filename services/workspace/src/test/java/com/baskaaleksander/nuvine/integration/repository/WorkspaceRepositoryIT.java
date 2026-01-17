package com.baskaaleksander.nuvine.integration.repository;

import com.baskaaleksander.nuvine.domain.model.Workspace;
import com.baskaaleksander.nuvine.integration.base.BaseRepositoryIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.TestDataBuilder;
import com.baskaaleksander.nuvine.infrastructure.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceRepositoryIT extends BaseRepositoryIntegrationTest {

    private static final UUID USER_1_ID = UUID.randomUUID();
    private static final UUID USER_2_ID = UUID.randomUUID();

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private TestDataBuilder testData;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        testData.cleanUp();
    }

    @Test
    void existsByNameAndOwnerId_matchesActiveWorkspace() {
        testData.createWorkspace("Workspace A", USER_1_ID, "user1@example.com");

        boolean exists = workspaceRepository.existsByNameAndOwnerId("Workspace A", USER_1_ID);
        boolean missing = workspaceRepository.existsByNameAndOwnerId("Missing", USER_1_ID);

        assertTrue(exists);
        assertFalse(missing);
    }

    @Test
    void updateWorkspaceName_updatesPersistedName() {
        Workspace workspace = testData.createWorkspace("Workspace A", USER_1_ID, "user1@example.com");

        workspaceRepository.updateWorkspaceName(workspace.getId(), "Updated Name");
        entityManager.flush();
        entityManager.clear();

        Workspace updated = workspaceRepository.findById(workspace.getId()).orElseThrow();
        assertEquals("Updated Name", updated.getName());
    }

    @Test
    void deleteWorkspace_marksWorkspaceDeleted() {
        Workspace workspace = testData.createWorkspace("Workspace A", USER_1_ID, "user1@example.com");

        workspaceRepository.deleteWorkspace(workspace.getId());
        entityManager.flush();
        entityManager.clear();

        Workspace deleted = workspaceRepository.findById(workspace.getId()).orElseThrow();
        assertTrue(deleted.isDeleted());
    }

    @Test
    void findAllByIdIn_returnsPagedActiveWorkspaces() {
        Workspace workspace1 = testData.createWorkspace("Workspace A", USER_1_ID, "user1@example.com");
        Workspace workspace2 = testData.createWorkspace("Workspace B", USER_2_ID, "user2@example.com");
        Workspace deleted = testData.createWorkspace("Workspace C", USER_1_ID, "user1@example.com");
        deleted.setDeleted(true);
        workspaceRepository.save(deleted);

        Page<Workspace> page = workspaceRepository.findAllByIdIn(
                List.of(workspace1.getId(), workspace2.getId(), deleted.getId()),
                PageRequest.of(0, 10)
        );

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().noneMatch(Workspace::isDeleted));
    }

    @Test
    void getWorkspaceNameById_returnsName() {
        Workspace workspace = testData.createWorkspace("Workspace A", USER_1_ID, "user1@example.com");

        String name = workspaceRepository.getWorkspaceNameById(workspace.getId()).orElse(null);

        assertNotNull(name);
        assertEquals("Workspace A", name);
    }
}
