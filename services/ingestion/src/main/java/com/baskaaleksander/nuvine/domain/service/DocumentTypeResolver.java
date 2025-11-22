package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentTypeResolver {

    public IngestionDocumentType resolve(String mimeType) {
        mimeType = mimeType.toLowerCase();
        return switch (mimeType) {
            case "application/pdf" -> IngestionDocumentType.PDF;
            case "text/plain" -> IngestionDocumentType.TEXT;
            case "text/markdown" -> IngestionDocumentType.MARKDOWN;
            case "text/html" -> IngestionDocumentType.HTML;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    IngestionDocumentType.DOCX;
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    IngestionDocumentType.PPTX;
            default -> IngestionDocumentType.UNKNOWN;
        };
    }
}
