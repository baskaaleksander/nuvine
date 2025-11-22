package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import com.baskaaleksander.nuvine.domain.service.extractor.DocumentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionService {

    private final DocumentTypeResolver typeResolver;
    private final List<DocumentExtractor> extractors;

    public ExtractedDocument extract(byte[] document, String mimeType) {

        log.info("EXTRACTION START");

        IngestionDocumentType type = typeResolver.resolve(mimeType);

        log.info("EXTRACTION TYPE RESOLVED type={}", type);

        return extractors.stream()
                .filter(extractor -> extractor.supports(type))
                .findFirst()
                .map(extractor -> extractor.extractText(document, mimeType))
                .orElseThrow(() -> {
                    log.info("EXTRACTION FAILED reason=unsupported_filetype");
                    return new IllegalArgumentException("Unsupported document type: " + type);
                });
    }
}
