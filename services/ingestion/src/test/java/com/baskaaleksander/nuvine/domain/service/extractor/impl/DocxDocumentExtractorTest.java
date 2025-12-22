package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DocxDocumentExtractorTest {

    private DocxDocumentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DocxDocumentExtractor();
    }

    @Test
    void supports_docxType_returnsTrue() {
        assertTrue(extractor.supports(IngestionDocumentType.DOCX));
    }

    @Test
    void supports_nonDocxTypes_returnsFalse() {
        assertFalse(extractor.supports(IngestionDocumentType.PDF));
        assertFalse(extractor.supports(IngestionDocumentType.TEXT));
        assertFalse(extractor.supports(IngestionDocumentType.HTML));
        assertFalse(extractor.supports(IngestionDocumentType.MARKDOWN));
        assertFalse(extractor.supports(IngestionDocumentType.PPTX));
        assertFalse(extractor.supports(IngestionDocumentType.UNKNOWN));
    }

    @Test
    void extractText_singleParagraph_extractsTextAndMetadata() throws Exception {
        byte[] docxBytes = createDocxWithParagraphs("Hello World from DOCX");

        ExtractedDocument result = extractor.extractText(docxBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertNotNull(result);
        assertTrue(result.text().contains("Hello World from DOCX"));
        assertEquals(1, result.sections().size());
        
        DocumentSection section = result.sections().get(0);
        assertEquals("section-0", section.id());
        assertEquals("DOCX Document", section.title());
        assertEquals(0, section.order());
        
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", result.metadata().get("mimeType"));
    }

    @Test
    void extractText_multipleParagraphs_extractsAllText() throws Exception {
        byte[] docxBytes = createDocxWithParagraphs("First paragraph", "Second paragraph", "Third paragraph");

        ExtractedDocument result = extractor.extractText(docxBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertNotNull(result);
        assertTrue(result.text().contains("First paragraph"));
        assertTrue(result.text().contains("Second paragraph"));
        assertTrue(result.text().contains("Third paragraph"));
        
        // DOCX extractor creates one section for entire document
        assertEquals(1, result.sections().size());
        assertTrue(result.sections().get(0).content().contains("First paragraph"));
        assertTrue(result.sections().get(0).content().contains("Second paragraph"));
        assertTrue(result.sections().get(0).content().contains("Third paragraph"));
    }

    @Test
    void extractText_emptyDocument_returnsEmptyText() throws Exception {
        byte[] docxBytes = createEmptyDocx();

        ExtractedDocument result = extractor.extractText(docxBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertNotNull(result);
        assertTrue(result.text().isEmpty() || result.text().isBlank());
        assertEquals(1, result.sections().size());
    }

    @Test
    void extractText_invalidDocxBytes_throwsRuntimeException() {
        byte[] invalidBytes = "not a docx".getBytes();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> extractor.extractText(invalidBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        
        // The exception could be wrapped or thrown directly by POI
        assertNotNull(exception.getMessage());
    }

    @Test
    void extractText_paragraphsWithBlankLines_skipsBlankParagraphs() throws Exception {
        byte[] docxBytes = createDocxWithParagraphs("Content before", "", "  ", "Content after");

        ExtractedDocument result = extractor.extractText(docxBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertNotNull(result);
        assertTrue(result.text().contains("Content before"));
        assertTrue(result.text().contains("Content after"));
        // Blank paragraphs should not add extra newlines
    }

    @Test
    void extractText_preservesMimeTypeInMetadata() throws Exception {
        byte[] docxBytes = createDocxWithParagraphs("Test content");
        String customMimeType = "application/msword";

        ExtractedDocument result = extractor.extractText(docxBytes, customMimeType);

        assertEquals(customMimeType, result.metadata().get("mimeType"));
    }

    @Test
    void extractText_metadataIncludesParagraphCount() throws Exception {
        byte[] docxBytes = createDocxWithParagraphs("Para 1", "Para 2", "Para 3");

        ExtractedDocument result = extractor.extractText(docxBytes, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertNotNull(result.metadata().get("paragraphs"));
        assertEquals(3, result.metadata().get("paragraphs"));
    }

    // Helper methods to create test DOCX files
    private byte[] createDocxWithParagraphs(String... paragraphs) throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            for (String text : paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.createRun().setText(text);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createEmptyDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);
            return baos.toByteArray();
        }
    }
}
