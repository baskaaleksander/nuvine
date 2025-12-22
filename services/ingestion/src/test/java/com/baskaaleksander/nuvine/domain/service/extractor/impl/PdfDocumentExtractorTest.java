package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfDocumentExtractorTest {

    private PdfDocumentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfDocumentExtractor();
    }

    @Test
    void supports_pdfType_returnsTrue() {
        assertTrue(extractor.supports(IngestionDocumentType.PDF));
    }

    @Test
    void supports_nonPdfTypes_returnsFalse() {
        assertFalse(extractor.supports(IngestionDocumentType.DOCX));
        assertFalse(extractor.supports(IngestionDocumentType.TEXT));
        assertFalse(extractor.supports(IngestionDocumentType.HTML));
        assertFalse(extractor.supports(IngestionDocumentType.MARKDOWN));
        assertFalse(extractor.supports(IngestionDocumentType.PPTX));
        assertFalse(extractor.supports(IngestionDocumentType.UNKNOWN));
    }

    @Test
    void extractText_singlePagePdf_extractsTextAndMetadata() throws Exception {
        byte[] pdfBytes = createPdfWithText("Hello World from PDF");

        ExtractedDocument result = extractor.extractText(pdfBytes, "application/pdf");

        assertNotNull(result);
        assertTrue(result.text().contains("Hello World from PDF"));
        assertEquals(1, result.sections().size());
        
        DocumentSection section = result.sections().get(0);
        assertEquals("page-1", section.id());
        assertEquals("Page 1", section.title());
        assertEquals(0, section.order());
        assertTrue(section.content().contains("Hello World from PDF"));
        
        assertEquals("application/pdf", result.metadata().get("mimeType"));
        assertEquals(1, result.metadata().get("pages"));
    }

    @Test
    void extractText_multiPagePdf_extractsAllPages() throws Exception {
        byte[] pdfBytes = createMultiPagePdf(new String[]{"Page One Content", "Page Two Content", "Page Three Content"});

        ExtractedDocument result = extractor.extractText(pdfBytes, "application/pdf");

        assertNotNull(result);
        assertTrue(result.text().contains("Page One Content"));
        assertTrue(result.text().contains("Page Two Content"));
        assertTrue(result.text().contains("Page Three Content"));
        
        assertEquals(3, result.sections().size());
        assertEquals(3, result.metadata().get("pages"));
        
        // Verify section ordering
        assertEquals("page-1", result.sections().get(0).id());
        assertEquals(0, result.sections().get(0).order());
        assertEquals("page-2", result.sections().get(1).id());
        assertEquals(1, result.sections().get(1).order());
        assertEquals("page-3", result.sections().get(2).id());
        assertEquals(2, result.sections().get(2).order());
    }

    @Test
    void extractText_emptyPdf_returnsEmptyTextWithZeroPages() throws Exception {
        byte[] pdfBytes = createEmptyPdf();

        ExtractedDocument result = extractor.extractText(pdfBytes, "application/pdf");

        assertNotNull(result);
        assertTrue(result.sections().isEmpty() || result.sections().get(0).content().isEmpty() || result.sections().get(0).content().isBlank());
        assertEquals(0, result.metadata().get("pages"));
    }

    @Test
    void extractText_invalidPdfBytes_throwsRuntimeException() {
        byte[] invalidBytes = "not a pdf".getBytes();

        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> extractor.extractText(invalidBytes, "application/pdf"));
        
        assertEquals("Failed to parse PDF", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void extractText_preservesMimeTypeInMetadata() throws Exception {
        byte[] pdfBytes = createPdfWithText("Test content");
        String customMimeType = "application/x-pdf";

        ExtractedDocument result = extractor.extractText(pdfBytes, customMimeType);

        assertEquals(customMimeType, result.metadata().get("mimeType"));
    }

    // Helper methods to create test PDFs
    private byte[] createPdfWithText(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createMultiPagePdf(String[] pageContents) throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (String content : pageContents) {
                PDPage page = new PDPage();
                document.addPage(page);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(100, 700);
                    contentStream.showText(content);
                    contentStream.endText();
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createEmptyPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
