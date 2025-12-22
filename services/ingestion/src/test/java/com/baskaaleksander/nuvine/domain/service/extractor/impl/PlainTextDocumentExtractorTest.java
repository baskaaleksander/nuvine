package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PlainTextDocumentExtractorTest {

    private PlainTextDocumentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PlainTextDocumentExtractor();
    }

    @Test
    void supports_textType_returnsTrue() {
        assertTrue(extractor.supports(IngestionDocumentType.TEXT));
    }

    @Test
    void supports_nonTextTypes_returnsFalse() {
        assertFalse(extractor.supports(IngestionDocumentType.PDF));
        assertFalse(extractor.supports(IngestionDocumentType.DOCX));
        assertFalse(extractor.supports(IngestionDocumentType.HTML));
        assertFalse(extractor.supports(IngestionDocumentType.MARKDOWN));
        assertFalse(extractor.supports(IngestionDocumentType.PPTX));
        assertFalse(extractor.supports(IngestionDocumentType.UNKNOWN));
    }

    @Test
    void extractText_simpleText_extractsTextAndMetadata() {
        String text = "Hello World from plain text";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(textBytes, "text/plain");

        assertNotNull(result);
        assertEquals("Hello World from plain text", result.text());
        assertEquals(1, result.sections().size());
        
        DocumentSection section = result.sections().get(0);
        assertEquals("section-0", section.id());
        assertEquals("Document body", section.title());
        assertEquals(0, section.order());
        assertEquals(text, section.content());
        
        assertEquals("text/plain", result.metadata().get("mimeType"));
        assertEquals(text.length(), result.metadata().get("length"));
    }

    @Test
    void extractText_multilineText_preservesNewlines() {
        String text = "Line 1\nLine 2\nLine 3";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(textBytes, "text/plain");

        assertNotNull(result);
        assertEquals(text, result.text());
        assertTrue(result.text().contains("\n"));
    }

    @Test
    void extractText_emptyText_returnsEmptyDocument() {
        String text = "";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(textBytes, "text/plain");

        assertNotNull(result);
        assertTrue(result.text().isEmpty());
        assertEquals(1, result.sections().size());
        assertEquals(0, result.metadata().get("length"));
    }

    @Test
    void extractText_preservesMimeTypeInMetadata() {
        String text = "Test content";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        String customMimeType = "text/csv";

        ExtractedDocument result = extractor.extractText(textBytes, customMimeType);

        assertEquals(customMimeType, result.metadata().get("mimeType"));
    }

    @Test
    void extractText_specialCharacters_preservesAllCharacters() {
        String text = "Special chars: @#$%^&*()_+-=[]{}|;':\",./<>?";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(textBytes, "text/plain");

        assertEquals(text, result.text());
    }

    @Test
    void extractText_unicodeText_handlesUtf8Correctly() {
        String text = "Unicode: 你好世界 مرحبا Привет 🎉";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(textBytes, "text/plain");

        assertEquals(text, result.text());
    }

    @Test
    void extractText_whitespacOnly_preservesWhitespace() {
        String text = "   \t\n   ";
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(textBytes, "text/plain");

        assertEquals(text, result.text());
        assertEquals(text.length(), result.metadata().get("length"));
    }

    @Test
    void extractText_largeText_calculatesCorrectLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Line ").append(i).append("\n");
        }
        String text = sb.toString();
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(textBytes, "text/plain");

        assertEquals(text, result.text());
        assertEquals(text.length(), result.metadata().get("length"));
    }
}
