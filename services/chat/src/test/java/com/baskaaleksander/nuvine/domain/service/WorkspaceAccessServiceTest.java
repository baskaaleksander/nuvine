package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceAccessServiceTest {

    @Mock
    private WorkspaceServiceClient workspaceServiceClient;

    @InjectMocks
    private WorkspaceAccessService workspaceAccessService;

    private UUID workspaceId;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
    }


    @Test
    void checkWorkspaceAccess_validWorkspace_completesSuccessfully() {
        doNothing().when(workspaceServiceClient).checkWorkspaceAccess(workspaceId);

        assertDoesNotThrow(() -> workspaceAccessService.checkWorkspaceAccess(workspaceId));

        verify(workspaceServiceClient).checkWorkspaceAccess(workspaceId);
    }

    @Test
    void checkWorkspaceAccess_notFound_throwsRuntimeException() {
        FeignException.NotFound notFoundException = createFeignException(404);
        doThrow(notFoundException).when(workspaceServiceClient).checkWorkspaceAccess(workspaceId);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> workspaceAccessService.checkWorkspaceAccess(workspaceId));

        assertEquals("WORKSPACE_NOT_FOUND", exception.getMessage());
        verify(workspaceServiceClient).checkWorkspaceAccess(workspaceId);
    }

    @Test
    void checkWorkspaceAccess_forbidden_throwsRuntimeException() {
        FeignException.Forbidden forbiddenException = createFeignException(403);
        doThrow(forbiddenException).when(workspaceServiceClient).checkWorkspaceAccess(workspaceId);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> workspaceAccessService.checkWorkspaceAccess(workspaceId));

        assertEquals("WORKSPACE_ACCESS_DENIED", exception.getMessage());
        verify(workspaceServiceClient).checkWorkspaceAccess(workspaceId);
    }

    @Test
    void checkWorkspaceAccess_otherFeignError_throwsRuntimeException() {
        FeignException.InternalServerError serverError = createFeignException(500);
        doThrow(serverError).when(workspaceServiceClient).checkWorkspaceAccess(workspaceId);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> workspaceAccessService.checkWorkspaceAccess(workspaceId));

        assertEquals("WORKSPACE_ACCESS_CHECK_FAILED", exception.getMessage());
        verify(workspaceServiceClient).checkWorkspaceAccess(workspaceId);
    }


    @Test
    void getDocumentIdsInProject_validProject_returnsIds() {
        List<UUID> expectedIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(workspaceServiceClient.getDocumentIdsInProject(projectId)).thenReturn(expectedIds);

        List<UUID> result = workspaceAccessService.getDocumentIdsInProject(projectId);

        assertEquals(expectedIds, result);
        verify(workspaceServiceClient).getDocumentIdsInProject(projectId);
    }

    @Test
    void getDocumentIdsInProject_notFound_throwsRuntimeException() {
        FeignException.NotFound notFoundException = createFeignException(404);
        when(workspaceServiceClient.getDocumentIdsInProject(projectId)).thenThrow(notFoundException);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> workspaceAccessService.getDocumentIdsInProject(projectId));

        assertEquals("PROJECT_NOT_FOUND", exception.getMessage());
        verify(workspaceServiceClient).getDocumentIdsInProject(projectId);
    }

    @Test
    void getDocumentIdsInProject_forbidden_throwsRuntimeException() {
        FeignException.Forbidden forbiddenException = createFeignException(403);
        when(workspaceServiceClient.getDocumentIdsInProject(projectId)).thenThrow(forbiddenException);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> workspaceAccessService.getDocumentIdsInProject(projectId));

        assertEquals("PROJECT_ACCESS_DENIED", exception.getMessage());
        verify(workspaceServiceClient).getDocumentIdsInProject(projectId);
    }


    @Test
    void validateRequestedDocuments_nullRequestedDocs_passes() {
        List<UUID> projectDocumentIds = List.of(UUID.randomUUID());

        assertDoesNotThrow(() ->
                workspaceAccessService.validateRequestedDocuments(null, projectDocumentIds, projectId));
    }

    @Test
    void validateRequestedDocuments_emptyRequestedDocs_passes() {
        List<UUID> projectDocumentIds = List.of(UUID.randomUUID());

        assertDoesNotThrow(() ->
                workspaceAccessService.validateRequestedDocuments(List.of(), projectDocumentIds, projectId));
    }

    @Test
    void validateRequestedDocuments_allPresent_passes() {
        UUID doc1 = UUID.randomUUID();
        UUID doc2 = UUID.randomUUID();
        List<UUID> requestedDocs = List.of(doc1, doc2);
        List<UUID> projectDocs = List.of(doc1, doc2, UUID.randomUUID());

        assertDoesNotThrow(() ->
                workspaceAccessService.validateRequestedDocuments(requestedDocs, projectDocs, projectId));
    }

    @Test
    void validateRequestedDocuments_missingDocs_throwsRuntimeException() {
        UUID doc1 = UUID.randomUUID();
        UUID doc2 = UUID.randomUUID();
        UUID missingDoc = UUID.randomUUID();
        List<UUID> requestedDocs = List.of(doc1, missingDoc);
        List<UUID> projectDocs = List.of(doc1, doc2);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> workspaceAccessService.validateRequestedDocuments(requestedDocs, projectDocs, projectId));

        assertEquals("DOCUMENTS_NOT_FOUND", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private <T extends FeignException> T createFeignException(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/test",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );

        return (T) FeignException.errorStatus("test", feign.Response.builder()
                .status(status)
                .reason("Test error")
                .request(request)
                .headers(Collections.emptyMap())
                .build());
    }
}
