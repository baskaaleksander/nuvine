package com.baskaaleksander.nuvine.domain.security;

import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("docAccess")
@RequiredArgsConstructor
public class DocumentAccessEvaluation {
    private final DocumentRepository documentRepository;
    private final ProjectAccessEvaluation projectAccessEvaluation;

    public boolean canGetDocument(UUID documentId, String userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        return projectAccessEvaluation.canGetProject(document.getProjectId(), userId);
    }

    public boolean canManageDocument(UUID documentId, String userId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL_SERVICE"))) {
            return true;
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found"));
        return projectAccessEvaluation.canManageProject(document.getProjectId(), userId);
    }
}
