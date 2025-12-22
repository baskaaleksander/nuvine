package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownDocumentExtractorTest {

    private MarkdownDocumentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new MarkdownDocumentExtractor();
    }

    @Test
    void supports_markdownType_returnsTrue() {
        assertTrue(extractor.supports(IngestionDocumentType.MARKDOWN));
    }

    @Test
    void supports_nonMarkdownTypes_returnsFalse() {
        assertFalse(extractor.supports(IngestionDocumentType.PDF));
        assertFalse(extractor.supports(IngestionDocumentType.TEXT));
        assertFalse(extractor.supports(IngestionDocumentType.DOCX));
        assertFalse(extractor.supports(IngestionDocumentType.HTML));
        assertFalse(extractor.supports(IngestionDocumentType.PPTX));
        assertFalse(extractor.supports(IngestionDocumentType.UNKNOWN));
    }

    @Test
    void extractText_simpleMarkdown_extractsTextAndMetadata() {
        String markdown = "Hello World from Markdown";
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(mdBytes, "text/markdown");

        assertNotNull(result);
        assertEquals("Hello World from Markdown", result.text());
        assertEquals(1, result.sections().size());
        
        DocumentSection section = result.sections().get(0);
        assertEquals("section-0", section.id());
        assertEquals("Markdown Body", section.title());
        assertEquals(0, section.order());
        
        assertEquals("text/markdown", result.metadata().get("mimeType"));
    }

    @Test
    void extractText_headings_stripsHashSymbols() {
        String markdown = """
                # Heading 1
                ## Heading 2
                ### Heading 3
                #### Heading 4
                ##### Heading 5
                ###### Heading 6
                """;
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(mdBytes, "text/markdown");

        assertNotNull(result);
        assertTrue(result.text().contains("Heading 1"));
        assertTrue(result.text().contains("Heading 2"));
        assertTrue(result.text().contains("Heading 3"));
        // Should not contain leading hash symbols
        assertFalse(result.text().contains("# "));
        assertFalse(result.text().contains("## "));
    }

    @Test
    void extractText_bulletLists_stripsListMarkers() {
        String markdown = """
                - Item 1
                - Item 2
                * Item 3
                + Item 4
                """;
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(mdBytes, "text/markdown");

        assertNotNull(result);
        assertTrue(result.text().contains("Item 1"));
        assertTrue(result.text().contains("Item 2"));
        assertTrue(result.text().contains("Item 3"));
        assertTrue(result.text().contains("Item 4"));
    }

    @Test
    void extractText_codeBlocks_stripsBackticks() {
        String markdown = """
                Inline `code` here
                
                ```
                code block
                ```
                """;
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(mdBytes, "text/markdown");

        assertNotNull(result);
        assertTrue(result.text().contains("code"));
        assertTrue(result.text().contains("code block"));
        // Should strip backticks
        assertFalse(result.text().contains("`"));
    }

    @Test
    void extractText_emptyMarkdown_returnsEmptyText() {
        String markdown = "";
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(mdBytes, "text/markdown");

        assertNotNull(result);
        assertTrue(result.text().isEmpty());
        assertEquals(1, result.sections().size());
    }

    @Test
    void extractText_preservesMimeTypeInMetadata() {
        String markdown = "Test content";
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);
        String customMimeType = "text/x-markdown";

        ExtractedDocument result = extractor.extractText(mdBytes, customMimeType);

        assertEquals(customMimeType, result.metadata().get("mimeType"));
    }

    @Test
    void extractText_metadataIncludesLengths() {
        String markdown = "# Title\n- item";
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(mdBytes, "text/markdown");

        assertEquals(markdown.length(), result.metadata().get("rawLength"));
        assertNotNull(result.metadata().get("cleanLength"));
        // Clean length should be smaller due to stripped markdown
        assertTrue((int) result.metadata().get("cleanLength") <= markdown.length());
    }

    @Test
    void extractText_mixedContent_extractsAllText() {
        String markdown = """
                # Main Title
                
                Some introductory text.
                
                ## Section 1
                
                - First point
                - Second point
                
                Here is some `inline code` and a paragraph.
                
                ```java
                public void main() {}
                ```
                
                ## Section 2
                
                More content here.
                """;
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(mdBytes, "text/markdown");

        assertNotNull(result);
        assertTrue(result.text().contains("Main Title"));
        assertTrue(result.text().contains("introductory text"));
        assertTrue(result.text().contains("Section 1"));
        assertTrue(result.text().contains("First point"));
        assertTrue(result.text().contains("inline code"));
        assertTrue(result.text().contains("public void main()"));
        assertTrue(result.text().contains("Section 2"));
    }

    @Test
    void extractText_preservesRegularText() {
        String markdown = "This is regular text without any markdown formatting.";
        byte[] mdBytes = markdown.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(mdBytes, "text/markdown");

        assertEquals("This is regular text without any markdown formatting.", result.text());
    }
}
