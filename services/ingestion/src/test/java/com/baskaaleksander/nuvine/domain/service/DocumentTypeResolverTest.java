package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTypeResolverTest {

    private DocumentTypeResolver documentTypeResolver;

    @BeforeEach
    void setUp() {
        documentTypeResolver = new DocumentTypeResolver();
    }

    @Test
    void resolve_applicationPdf_returnsPDF() {
        IngestionDocumentType result = documentTypeResolver.resolve("application/pdf");

        assertEquals(IngestionDocumentType.PDF, result);
    }

    @Test
    void resolve_applicationPdfUpperCase_returnsPDF() {
        IngestionDocumentType result = documentTypeResolver.resolve("APPLICATION/PDF");

        assertEquals(IngestionDocumentType.PDF, result);
    }

    @Test
    void resolve_textPlain_returnsTEXT() {
        IngestionDocumentType result = documentTypeResolver.resolve("text/plain");

        assertEquals(IngestionDocumentType.TEXT, result);
    }

    @Test
    void resolve_textMarkdown_returnsMARKDOWN() {
        IngestionDocumentType result = documentTypeResolver.resolve("text/markdown");

        assertEquals(IngestionDocumentType.MARKDOWN, result);
    }

    @Test
    void resolve_textHtml_returnsHTML() {
        IngestionDocumentType result = documentTypeResolver.resolve("text/html");

        assertEquals(IngestionDocumentType.HTML, result);
    }

    @Test
    void resolve_applicationDocx_returnsDOCX() {
        IngestionDocumentType result = documentTypeResolver.resolve(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertEquals(IngestionDocumentType.DOCX, result);
    }

    @Test
    void resolve_applicationPptx_returnsPPTX() {
        IngestionDocumentType result = documentTypeResolver.resolve(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        assertEquals(IngestionDocumentType.PPTX, result);
    }

    @Test
    void resolve_unknownMimeType_returnsUNKNOWN() {
        IngestionDocumentType result = documentTypeResolver.resolve("application/octet-stream");

        assertEquals(IngestionDocumentType.UNKNOWN, result);
    }

    @Test
    void resolve_emptyString_returnsUNKNOWN() {
        IngestionDocumentType result = documentTypeResolver.resolve("");

        assertEquals(IngestionDocumentType.UNKNOWN, result);
    }

    @Test
    void resolve_randomString_returnsUNKNOWN() {
        IngestionDocumentType result = documentTypeResolver.resolve("some-random-type");

        assertEquals(IngestionDocumentType.UNKNOWN, result);
    }
}
