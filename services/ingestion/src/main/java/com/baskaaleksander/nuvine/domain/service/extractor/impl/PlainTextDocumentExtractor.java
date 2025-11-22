package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import com.baskaaleksander.nuvine.domain.service.extractor.DocumentExtractor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class PlainTextDocumentExtractor implements DocumentExtractor {
    @Override
    public boolean supports(IngestionDocumentType type) {
        return type == IngestionDocumentType.TEXT;
    }

    @Override
    public ExtractedDocument extractText(byte[] document, String mimeType) {

        String text = new String(document, StandardCharsets.UTF_8);

        List<DocumentSection> sections = List.of(
                new DocumentSection(
                        "section-0",
                        "Document body",
                        0,
                        text
                )
        );

        Map<String, Object> metadata = Map.of(
                "mimeType", mimeType,
                "length", text.length()
        );

        return new ExtractedDocument(
                text,
                sections,
                metadata
        );
    }
}
