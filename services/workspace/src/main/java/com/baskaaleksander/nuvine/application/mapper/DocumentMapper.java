package com.baskaaleksander.nuvine.application.mapper;

import com.baskaaleksander.nuvine.application.dto.DocumentResponse;
import com.baskaaleksander.nuvine.domain.model.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toDocumentResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getProjectId(),
                document.getWorkspaceId(),
                document.getName(),
                document.getStatus(),
                document.getCreatedBy(),
                document.getCreatedAt()
        );
    }
}
