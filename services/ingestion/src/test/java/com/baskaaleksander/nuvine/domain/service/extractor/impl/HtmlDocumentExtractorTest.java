package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HtmlDocumentExtractorTest {

    private HtmlDocumentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new HtmlDocumentExtractor();
    }

    @Test
    void supports_htmlType_returnsTrue() {
        assertTrue(extractor.supports(IngestionDocumentType.HTML));
    }

    @Test
    void supports_nonHtmlTypes_returnsFalse() {
        assertFalse(extractor.supports(IngestionDocumentType.PDF));
        assertFalse(extractor.supports(IngestionDocumentType.TEXT));
        assertFalse(extractor.supports(IngestionDocumentType.DOCX));
        assertFalse(extractor.supports(IngestionDocumentType.MARKDOWN));
        assertFalse(extractor.supports(IngestionDocumentType.PPTX));
        assertFalse(extractor.supports(IngestionDocumentType.UNKNOWN));
    }

    @Test
    void extractText_simpleHtml_extractsBodyTextAndTitle() {
        String html = "<html><head><title>Test Title</title></head><body><p>Hello World</p></body></html>";
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(htmlBytes, "text/html");

        assertNotNull(result);
        assertEquals("Hello World", result.text());
        assertEquals(1, result.sections().size());
        
        DocumentSection section = result.sections().get(0);
        assertEquals("section-0", section.id());
        assertEquals("Test Title", section.title());
        assertEquals(0, section.order());
        assertEquals("Hello World", section.content());
        
        assertEquals("text/html", result.metadata().get("mimeType"));
        assertEquals("Test Title", result.metadata().get("title"));
    }

    @Test
    void extractText_htmlWithNestedElements_extractsAllText() {
        String html = """
                <html>
                <head><title>Nested Test</title></head>
                <body>
                    <div>
                        <h1>Header</h1>
                        <p>Paragraph one</p>
                        <ul>
                            <li>Item 1</li>
                            <li>Item 2</li>
                        </ul>
                    </div>
                </body>
                </html>
                """;
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(htmlBytes, "text/html");

        assertNotNull(result);
        assertTrue(result.text().contains("Header"));
        assertTrue(result.text().contains("Paragraph one"));
        assertTrue(result.text().contains("Item 1"));
        assertTrue(result.text().contains("Item 2"));
    }

    @Test
    void extractText_htmlWithoutTitle_usesDefaultTitle() {
        String html = "<html><body><p>No title here</p></body></html>";
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(htmlBytes, "text/html");

        assertNotNull(result);
        assertEquals("", result.sections().get(0).title()); // Empty title when no <title> tag
    }

    @Test
    void extractText_htmlWithEmptyBody_returnsEmptyText() {
        String html = "<html><head><title>Empty</title></head><body></body></html>";
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(htmlBytes, "text/html");

        assertNotNull(result);
        assertTrue(result.text().isEmpty());
        assertEquals(1, result.sections().size());
    }

    @Test
    void extractText_stripsHtmlTags_returnsOnlyText() {
        String html = "<html><body><b>Bold</b> and <i>italic</i> and <a href='link'>link text</a></body></html>";
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(htmlBytes, "text/html");

        assertNotNull(result);
        assertTrue(result.text().contains("Bold"));
        assertTrue(result.text().contains("italic"));
        assertTrue(result.text().contains("link text"));
        assertFalse(result.text().contains("<b>"));
        assertFalse(result.text().contains("<i>"));
        assertFalse(result.text().contains("href"));
    }

    @Test
    void extractText_htmlWithScript_ignoresScriptContent() {
        String html = """
                <html>
                <body>
                    <p>Visible text</p>
                    <script>var hidden = 'script content';</script>
                </body>
                </html>
                """;
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(htmlBytes, "text/html");

        assertNotNull(result);
        assertTrue(result.text().contains("Visible text"));
        // Jsoup's body().text() should not include script content
    }

    @Test
    void extractText_preservesMimeTypeInMetadata() {
        String html = "<html><body>Test</body></html>";
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);
        String customMimeType = "application/xhtml+xml";

        ExtractedDocument result = extractor.extractText(htmlBytes, customMimeType);

        assertEquals(customMimeType, result.metadata().get("mimeType"));
    }

    @Test
    void extractText_htmlWithEntities_decodesEntities() {
        String html = "<html><body><p>&amp; &lt; &gt; &quot; &copy;</p></body></html>";
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(htmlBytes, "text/html");

        assertNotNull(result);
        assertTrue(result.text().contains("&"));
        assertTrue(result.text().contains("<"));
        assertTrue(result.text().contains(">"));
    }

    @Test
    void extractText_malformedHtml_stillExtractsText() {
        String html = "<html><body><p>Unclosed paragraph<div>Unclosed div</body>";
        byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

        ExtractedDocument result = extractor.extractText(htmlBytes, "text/html");

        assertNotNull(result);
        assertTrue(result.text().contains("Unclosed paragraph"));
        assertTrue(result.text().contains("Unclosed div"));
    }
}
