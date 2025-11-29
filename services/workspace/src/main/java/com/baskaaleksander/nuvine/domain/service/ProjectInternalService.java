package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.infrastructure.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectInternalService {

    private final DocumentRepository documentRepository;

    public List<UUID> getDocumentIdsInProject(UUID projectId) {
        return documentRepository.findDocIdsByProjectId(projectId);
    }
}
