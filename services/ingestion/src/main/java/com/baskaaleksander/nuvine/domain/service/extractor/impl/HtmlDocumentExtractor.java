package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import com.baskaaleksander.nuvine.domain.service.extractor.DocumentExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class HtmlDocumentExtractor implements DocumentExtractor {

    @Override
    public boolean supports(IngestionDocumentType type) {
        return type == IngestionDocumentType.HTML;
    }

    @Override
    public ExtractedDocument extractText(byte[] document, String mimeType) {
        String html = new String(document, StandardCharsets.UTF_8);

        Document parsed = Jsoup.parse(html);

        String bodyText = parsed.body() != null ? parsed.body().text() : "";
        String title = parsed.title() != null ? parsed.title() : "HTML Document";

        DocumentSection section = new DocumentSection(
                "section-0",
                title,
                0,
                bodyText
        );

        return new ExtractedDocument(
                bodyText,
                List.of(section),
                Map.of(
                        "mimeType", mimeType,
                        "title", title
                )
        );
    }
}
