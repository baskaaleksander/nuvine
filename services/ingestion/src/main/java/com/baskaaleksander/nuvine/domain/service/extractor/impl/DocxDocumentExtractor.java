package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import com.baskaaleksander.nuvine.domain.service.extractor.DocumentExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class DocxDocumentExtractor implements DocumentExtractor {

    @Override
    public boolean supports(IngestionDocumentType type) {
        return type == IngestionDocumentType.DOCX;
    }

    @Override
    public ExtractedDocument extractText(byte[] document, String mimeType) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(document))) {

            StringBuilder sb = new StringBuilder();

            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String txt = paragraph.getText();
                if (txt != null && !txt.isBlank()) {
                    sb.append(txt).append("\n");
                }
            }

            String text = sb.toString().trim();

            DocumentSection section = new DocumentSection(
                    "section-0",
                    "DOCX Document",
                    0,
                    text
            );

            return new ExtractedDocument(
                    text,
                    List.of(section),
                    Map.of(
                            "mimeType", mimeType,
                            "paragraphs", doc.getParagraphs().size()
                    )
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse DOCX", e);
        }
    }
}
