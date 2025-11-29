package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceAccessService {

    private final WorkspaceServiceClient workspaceServiceClient;

    public void checkWorkspaceAccess(UUID workspaceId) {
        log.info("WORKSPACE_ACCESS_CHECK START workspaceId={}", workspaceId);
        try {
            workspaceServiceClient.checkWorkspaceAccess(workspaceId);
            log.info("WORKSPACE_ACCESS_CHECK END workspaceId={} status=OK", workspaceId);
        } catch (FeignException e) {
            int status = e.status();
            if (status == 404) {
                log.warn("WORKSPACE_ACCESS_CHECK END workspaceId={} status=NOT_FOUND", workspaceId);
                throw new RuntimeException("WORKSPACE_NOT_FOUND");
            } else if (status == 403) {
                log.warn("WORKSPACE_ACCESS_CHECK END workspaceId={} status=FORBIDDEN", workspaceId);
                throw new RuntimeException("WORKSPACE_ACCESS_DENIED");
            }
            log.error("WORKSPACE_ACCESS_CHECK FAILED workspaceId={} status={}", workspaceId, status, e);
            throw new RuntimeException("WORKSPACE_ACCESS_CHECK_FAILED", e);
        } catch (Exception e) {
            log.error("WORKSPACE_ACCESS_CHECK FAILED workspaceId={}", workspaceId, e);
            throw new RuntimeException("WORKSPACE_ACCESS_CHECK_FAILED", e);
        }
    }

    public List<UUID> getDocumentIdsInProject(UUID projectId) {
        log.info("PROJECT_DOCUMENTS_FETCH START projectId={}", projectId);
        try {
            List<UUID> docs = workspaceServiceClient.getDocumentIdsInProject(projectId);
            log.info(
                    "PROJECT_DOCUMENTS_FETCH END projectId={} documentsCount={}",
                    projectId,
                    docs.size()
            );
            return docs;
        } catch (FeignException e) {
            int status = e.status();
            if (status == 404) {
                log.warn("PROJECT_DOCUMENTS_FETCH END projectId={} status=NOT_FOUND", projectId);
                throw new RuntimeException("PROJECT_NOT_FOUND");
            } else if (status == 403) {
                log.warn("PROJECT_DOCUMENTS_FETCH END projectId={} status=FORBIDDEN", projectId);
                throw new RuntimeException("PROJECT_ACCESS_DENIED");
            }
            log.error("PROJECT_DOCUMENTS_FETCH FAILED projectId={} status={}", projectId, status, e);
            throw new RuntimeException("PROJECT_ACCESS_CHECK_FAILED", e);
        } catch (Exception e) {
            log.error("PROJECT_DOCUMENTS_FETCH FAILED projectId={}", projectId, e);
            throw new RuntimeException("PROJECT_ACCESS_CHECK_FAILED", e);
        }
    }

    public void validateRequestedDocuments(
            List<UUID> requestedDocumentIds,
            List<UUID> projectDocumentIds,
            UUID projectId
    ) {
        if (requestedDocumentIds == null || requestedDocumentIds.isEmpty()) {
            log.info(
                    "PROJECT_DOCUMENTS_VALIDATE SKIP projectId={} reason=NO_REQUESTED_DOCUMENTS",
                    projectId
            );
            return;
        }

        Set<UUID> available = new HashSet<>(projectDocumentIds);

        List<UUID> missing = requestedDocumentIds.stream()
                .filter(id -> !available.contains(id))
                .toList();

        if (!missing.isEmpty()) {
            log.warn(
                    "PROJECT_DOCUMENTS_VALIDATE FAILED projectId={} requestedCount={} availableCount={} missingCount={}",
                    projectId,
                    requestedDocumentIds.size(),
                    projectDocumentIds.size(),
                    missing.size()
            );
            throw new RuntimeException("DOCUMENTS_NOT_FOUND");
        }

        log.info(
                "PROJECT_DOCUMENTS_VALIDATE OK projectId={} requestedCount={} availableCount={}",
                projectId,
                requestedDocumentIds.size(),
                projectDocumentIds.size()
        );
    }
}