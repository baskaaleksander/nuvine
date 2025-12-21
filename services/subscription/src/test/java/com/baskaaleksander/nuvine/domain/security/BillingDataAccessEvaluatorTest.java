package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.TestFixtures;
import com.baskaaleksander.nuvine.application.dto.WorkspaceMemberResponse;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceUserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingDataAccessEvaluator")
class BillingDataAccessEvaluatorTest {

    @Mock
    private WorkspaceServiceUserClient workspaceServiceUserClient;

    @InjectMocks
    private BillingDataAccessEvaluator evaluator;

    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        workspaceId = TestFixtures.DEFAULT_WORKSPACE_ID;
    }

    @Test
    @DisplayName("canAccessBillingData returns true for workspace owner")
    void canAccessBillingData_returnsTrue_forOwner() {
        WorkspaceMemberResponse ownerResponse = TestFixtures.ownerMemberResponse();
        when(workspaceServiceUserClient.getWorkspaceMember(workspaceId)).thenReturn(ownerResponse);

        boolean result = evaluator.canAccessBillingData(workspaceId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccessBillingData returns false for workspace member")
    void canAccessBillingData_returnsFalse_forMember() {
        WorkspaceMemberResponse memberResponse = TestFixtures.memberMemberResponse();
        when(workspaceServiceUserClient.getWorkspaceMember(workspaceId)).thenReturn(memberResponse);

        boolean result = evaluator.canAccessBillingData(workspaceId);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("canAccessBillingData returns false for workspace moderator")
    void canAccessBillingData_returnsFalse_forModerator() {
        WorkspaceMemberResponse moderatorResponse = TestFixtures.moderatorMemberResponse();
        when(workspaceServiceUserClient.getWorkspaceMember(workspaceId)).thenReturn(moderatorResponse);

        boolean result = evaluator.canAccessBillingData(workspaceId);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("canAccessBillingData returns false for workspace viewer")
    void canAccessBillingData_returnsFalse_forViewer() {
        WorkspaceMemberResponse viewerResponse = TestFixtures.viewerMemberResponse();
        when(workspaceServiceUserClient.getWorkspaceMember(workspaceId)).thenReturn(viewerResponse);

        boolean result = evaluator.canAccessBillingData(workspaceId);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("canAccessBillingData returns false when client throws exception")
    void canAccessBillingData_returnsFalse_whenClientThrowsException() {
        when(workspaceServiceUserClient.getWorkspaceMember(workspaceId))
                .thenThrow(new RuntimeException("Service unavailable"));

        boolean result = evaluator.canAccessBillingData(workspaceId);

        assertThat(result).isFalse();
    }
}
