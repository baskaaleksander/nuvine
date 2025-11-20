package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.WorkspaceCreateResponse;
import com.baskaaleksander.nuvine.application.dto.WorkspaceResponse;
import com.baskaaleksander.nuvine.domain.model.BillingTier;
import com.baskaaleksander.nuvine.domain.model.Workspace;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkspaceMapperTest {

    private final WorkspaceMapper mapper = new WorkspaceMapper();

    @Test
    void toWorkspaceCreateResponse_mapsAllFields() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Workspace workspace = Workspace.builder()
                .id(id)
                .name("Workspace")
                .ownerUserId(ownerId)
                .billingTier(BillingTier.FREE)
                .createdAt(createdAt)
                .build();

        WorkspaceCreateResponse response = mapper.toWorkspaceCreateResponse(workspace);

        assertEquals(id, response.id());
        assertEquals("Workspace", response.name());
        assertEquals(ownerId, response.ownerUserId());
        assertEquals(BillingTier.FREE, response.billingTier());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void toWorkspaceResponse_mapsAllFields_includingNullableSubscription() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();

        Workspace workspace = Workspace.builder()
                .id(id)
                .name("Workspace")
                .ownerUserId(ownerId)
                .billingTier(BillingTier.PRO)
                .subscriptionId(null)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        WorkspaceResponse response = mapper.toWorkspaceResponse(workspace);

        assertEquals(id, response.id());
        assertEquals("Workspace", response.name());
        assertEquals(ownerId, response.ownerUserId());
        assertNull(response.subscriptionId());
        assertEquals(BillingTier.PRO, response.billingTier());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }
}
