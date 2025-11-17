package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentResponse;
import com.baskaaleksander.nuvine.application.mapper.DocumentMapper;
import com.baskaaleksander.nuvine.domain.exception.ProjectNotFoundException;
import com.baskaaleksander.nuvine.domain.model.Document;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.domain.model.Project;
import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import com.baskaaleksander.nuvine.infrastructure.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final DocumentMapper documentMapper;

    public DocumentResponse createDocument(String name, UUID userId, UUID projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        Document document = Document.builder()
                .projectId(projectId)
                .workspaceId(project.getWorkspaceId())
                .name(name)
                .status(DocumentStatus.UPLOADING)
                .createdBy(userId)
                .projectId(projectId)
                .build();

        return documentMapper.toDocumentResponse(documentRepository.save(document));
    }
}
