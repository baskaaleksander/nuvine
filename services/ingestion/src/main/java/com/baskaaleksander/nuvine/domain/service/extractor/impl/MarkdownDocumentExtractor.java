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
public class MarkdownDocumentExtractor implements DocumentExtractor {

    @Override
    public boolean supports(IngestionDocumentType type) {
        return type == IngestionDocumentType.MARKDOWN;
    }

    @Override
    public ExtractedDocument extractText(byte[] document, String mimeType) {
        String raw = new String(document, StandardCharsets.UTF_8);

        String text = raw
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s*[-*+]\\s*", "")
                .replaceAll("`{1,3}", "");

        DocumentSection section = new DocumentSection(
                "section-0",
                "Markdown Body",
                0,
                text
        );

        Map<String, Object> metadata = Map.of(
                "mimeType", mimeType,
                "rawLength", raw.length(),
                "cleanLength", text.length()
        );

        return new ExtractedDocument(
                text,
                List.of(section),
                metadata
        );
    }
}
