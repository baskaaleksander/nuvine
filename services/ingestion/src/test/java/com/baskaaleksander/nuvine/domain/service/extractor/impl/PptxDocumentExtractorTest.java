package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PptxDocumentExtractorTest {

    private PptxDocumentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PptxDocumentExtractor();
    }

    @Test
    void supports_pptxType_returnsTrue() {
        assertTrue(extractor.supports(IngestionDocumentType.PPTX));
    }

    @Test
    void supports_nonPptxTypes_returnsFalse() {
        assertFalse(extractor.supports(IngestionDocumentType.PDF));
        assertFalse(extractor.supports(IngestionDocumentType.TEXT));
        assertFalse(extractor.supports(IngestionDocumentType.HTML));
        assertFalse(extractor.supports(IngestionDocumentType.MARKDOWN));
        assertFalse(extractor.supports(IngestionDocumentType.DOCX));
        assertFalse(extractor.supports(IngestionDocumentType.UNKNOWN));
    }

    @Test
    void extractText_singleSlide_extractsTextAndMetadata() throws Exception {
        byte[] pptxBytes = createPptxWithSlides(new String[][]{{"Slide Title", "Slide Content"}});

        ExtractedDocument result = extractor.extractText(pptxBytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        assertNotNull(result);
        assertTrue(result.text().contains("Slide Title"));
        assertTrue(result.text().contains("Slide Content"));
        assertEquals(1, result.sections().size());
        
        DocumentSection section = result.sections().get(0);
        assertEquals("slide-1", section.id());
        assertEquals("Slide Title", section.title()); // First text shape becomes title
        assertEquals(0, section.order());
        assertTrue(section.content().contains("Slide Content"));
        
        assertEquals("application/vnd.openxmlformats-officedocument.presentationml.presentation", result.metadata().get("mimeType"));
        assertEquals(1, result.metadata().get("slides"));
    }

    @Test
    void extractText_multipleSlides_extractsAllSlides() throws Exception {
        byte[] pptxBytes = createPptxWithSlides(new String[][]{
                {"Title 1", "Content 1"},
                {"Title 2", "Content 2"},
                {"Title 3", "Content 3"}
        });

        ExtractedDocument result = extractor.extractText(pptxBytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        assertNotNull(result);
        assertEquals(3, result.sections().size());
        assertEquals(3, result.metadata().get("slides"));
        
        // Verify slide ordering
        assertEquals("slide-1", result.sections().get(0).id());
        assertEquals(0, result.sections().get(0).order());
        assertEquals("slide-2", result.sections().get(1).id());
        assertEquals(1, result.sections().get(1).order());
        assertEquals("slide-3", result.sections().get(2).id());
        assertEquals(2, result.sections().get(2).order());
    }

    @Test
    void extractText_emptyPresentation_returnsEmptyContent() throws Exception {
        byte[] pptxBytes = createEmptyPptx();

        ExtractedDocument result = extractor.extractText(pptxBytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        assertNotNull(result);
        assertTrue(result.sections().isEmpty());
        assertEquals(0, result.metadata().get("slides"));
    }

    @Test
    void extractText_invalidPptxBytes_throwsRuntimeException() {
        byte[] invalidBytes = "not a pptx".getBytes();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> extractor.extractText(invalidBytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        
        assertEquals("Failed to parse PPTX", exception.getMessage());
        assertNotNull(exception.getCause());
    }

    @Test
    void extractText_slideWithoutTitle_usesSlideNumberAsTitle() throws Exception {
        byte[] pptxBytes = createPptxWithSingleTextShape("Only content, no title");

        ExtractedDocument result = extractor.extractText(pptxBytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        assertNotNull(result);
        assertEquals(1, result.sections().size());
        // First text becomes title, so title should be the content
        assertEquals("Only content, no title", result.sections().get(0).title());
    }

    @Test
    void extractText_preservesMimeTypeInMetadata() throws Exception {
        byte[] pptxBytes = createPptxWithSlides(new String[][]{{"Title", "Content"}});
        String customMimeType = "application/powerpoint";

        ExtractedDocument result = extractor.extractText(pptxBytes, customMimeType);

        assertEquals(customMimeType, result.metadata().get("mimeType"));
    }

    @Test
    void extractText_formattedGlobalText_includesSlideMarkers() throws Exception {
        byte[] pptxBytes = createPptxWithSlides(new String[][]{
                {"Introduction", "Welcome to the presentation"},
                {"Main Point", "Here is the main content"}
        });

        ExtractedDocument result = extractor.extractText(pptxBytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        assertNotNull(result);
        // Global text should include slide markers [Title]
        assertTrue(result.text().contains("[Introduction]"));
        assertTrue(result.text().contains("[Main Point]"));
    }

    // Helper methods to create test PPTX files
    private byte[] createPptxWithSlides(String[][] slidesContent) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            for (String[] slideContent : slidesContent) {
                XSLFSlide slide = ppt.createSlide();
                
                double yOffset = 50;
                for (String text : slideContent) {
                    XSLFTextBox textBox = slide.createTextBox();
                    textBox.setAnchor(new Rectangle2D.Double(50, yOffset, 400, 50));
                    textBox.setText(text);
                    yOffset += 60;
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ppt.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createPptxWithSingleTextShape(String text) throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            XSLFSlide slide = ppt.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle2D.Double(50, 50, 400, 50));
            textBox.setText(text);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ppt.write(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createEmptyPptx() throws Exception {
        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ppt.write(baos);
            return baos.toByteArray();
        }
    }
}
