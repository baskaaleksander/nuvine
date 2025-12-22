package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import com.baskaaleksander.nuvine.domain.service.extractor.DocumentExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractionServiceTest {

    @Mock
    private DocumentTypeResolver typeResolver;

    @Mock
    private DocumentExtractor pdfExtractor;

    @Mock
    private DocumentExtractor docxExtractor;

    @Mock
    private DocumentExtractor htmlExtractor;

    private ExtractionService extractionService;

    private byte[] documentBytes;
    private ExtractedDocument extractedDocument;

    @BeforeEach
    void setUp() {
        documentBytes = "test content".getBytes();
        extractedDocument = new ExtractedDocument("Extracted text", List.of(), Map.of());
    }

    @Test
    void extract_pdfMimeType_delegatesToPdfExtractor() {
        String mimeType = "application/pdf";
        List<DocumentExtractor> extractors = List.of(pdfExtractor, docxExtractor, htmlExtractor);
        extractionService = new ExtractionService(typeResolver, extractors);

        when(typeResolver.resolve(mimeType)).thenReturn(IngestionDocumentType.PDF);
        when(pdfExtractor.supports(IngestionDocumentType.PDF)).thenReturn(true);
        when(pdfExtractor.extractText(documentBytes, mimeType)).thenReturn(extractedDocument);

        ExtractedDocument result = extractionService.extract(documentBytes, mimeType);

        assertEquals(extractedDocument, result);
        verify(typeResolver).resolve(mimeType);
        verify(pdfExtractor).supports(IngestionDocumentType.PDF);
        verify(pdfExtractor).extractText(documentBytes, mimeType);
    }

    @Test
    void extract_docxMimeType_delegatesToDocxExtractor() {
        String mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        List<DocumentExtractor> extractors = List.of(pdfExtractor, docxExtractor, htmlExtractor);
        extractionService = new ExtractionService(typeResolver, extractors);

        when(typeResolver.resolve(mimeType)).thenReturn(IngestionDocumentType.DOCX);
        when(pdfExtractor.supports(IngestionDocumentType.DOCX)).thenReturn(false);
        when(docxExtractor.supports(IngestionDocumentType.DOCX)).thenReturn(true);
        when(docxExtractor.extractText(documentBytes, mimeType)).thenReturn(extractedDocument);

        ExtractedDocument result = extractionService.extract(documentBytes, mimeType);

        assertEquals(extractedDocument, result);
        verify(docxExtractor).extractText(documentBytes, mimeType);
    }

    @Test
    void extract_htmlMimeType_delegatesToHtmlExtractor() {
        String mimeType = "text/html";
        List<DocumentExtractor> extractors = List.of(pdfExtractor, docxExtractor, htmlExtractor);
        extractionService = new ExtractionService(typeResolver, extractors);

        when(typeResolver.resolve(mimeType)).thenReturn(IngestionDocumentType.HTML);
        when(pdfExtractor.supports(IngestionDocumentType.HTML)).thenReturn(false);
        when(docxExtractor.supports(IngestionDocumentType.HTML)).thenReturn(false);
        when(htmlExtractor.supports(IngestionDocumentType.HTML)).thenReturn(true);
        when(htmlExtractor.extractText(documentBytes, mimeType)).thenReturn(extractedDocument);

        ExtractedDocument result = extractionService.extract(documentBytes, mimeType);

        assertEquals(extractedDocument, result);
        verify(htmlExtractor).extractText(documentBytes, mimeType);
    }

    @Test
    void extract_unsupportedMimeType_throwsIllegalArgumentException() {
        String mimeType = "application/octet-stream";
        List<DocumentExtractor> extractors = List.of(pdfExtractor, docxExtractor, htmlExtractor);
        extractionService = new ExtractionService(typeResolver, extractors);

        when(typeResolver.resolve(mimeType)).thenReturn(IngestionDocumentType.UNKNOWN);
        when(pdfExtractor.supports(IngestionDocumentType.UNKNOWN)).thenReturn(false);
        when(docxExtractor.supports(IngestionDocumentType.UNKNOWN)).thenReturn(false);
        when(htmlExtractor.supports(IngestionDocumentType.UNKNOWN)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractionService.extract(documentBytes, mimeType)
        );
        assertTrue(exception.getMessage().contains("Unsupported document type"));
    }

    @Test
    void extract_noMatchingExtractor_throwsIllegalArgumentException() {
        String mimeType = "application/pdf";
        List<DocumentExtractor> extractors = List.of(docxExtractor, htmlExtractor); // No PDF extractor
        extractionService = new ExtractionService(typeResolver, extractors);

        when(typeResolver.resolve(mimeType)).thenReturn(IngestionDocumentType.PDF);
        when(docxExtractor.supports(IngestionDocumentType.PDF)).thenReturn(false);
        when(htmlExtractor.supports(IngestionDocumentType.PDF)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extractionService.extract(documentBytes, mimeType)
        );
        assertTrue(exception.getMessage().contains("PDF"));
    }

    @Test
    void extract_firstMatchingExtractorIsUsed() {
        String mimeType = "application/pdf";
        DocumentExtractor anotherPdfExtractor = mock(DocumentExtractor.class);
        List<DocumentExtractor> extractors = List.of(pdfExtractor, anotherPdfExtractor);
        extractionService = new ExtractionService(typeResolver, extractors);

        when(typeResolver.resolve(mimeType)).thenReturn(IngestionDocumentType.PDF);
        when(pdfExtractor.supports(IngestionDocumentType.PDF)).thenReturn(true);
        when(pdfExtractor.extractText(documentBytes, mimeType)).thenReturn(extractedDocument);

        extractionService.extract(documentBytes, mimeType);

        verify(pdfExtractor).extractText(documentBytes, mimeType);
        verify(anotherPdfExtractor, never()).extractText(any(), any());
    }

    @Test
    void extract_emptyExtractorList_throwsException() {
        String mimeType = "application/pdf";
        List<DocumentExtractor> extractors = List.of();
        extractionService = new ExtractionService(typeResolver, extractors);

        when(typeResolver.resolve(mimeType)).thenReturn(IngestionDocumentType.PDF);

        assertThrows(IllegalArgumentException.class,
                () -> extractionService.extract(documentBytes, mimeType));
    }

    @Test
    void extract_extractorThrowsException_propagatesException() {
        String mimeType = "application/pdf";
        List<DocumentExtractor> extractors = List.of(pdfExtractor);
        extractionService = new ExtractionService(typeResolver, extractors);

        when(typeResolver.resolve(mimeType)).thenReturn(IngestionDocumentType.PDF);
        when(pdfExtractor.supports(IngestionDocumentType.PDF)).thenReturn(true);
        when(pdfExtractor.extractText(documentBytes, mimeType))
                .thenThrow(new RuntimeException("Extraction failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> extractionService.extract(documentBytes, mimeType)
        );
        assertEquals("Extraction failed", exception.getMessage());
    }
}
