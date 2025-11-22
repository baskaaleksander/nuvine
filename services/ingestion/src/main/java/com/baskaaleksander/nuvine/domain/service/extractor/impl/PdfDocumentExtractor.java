package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import com.baskaaleksander.nuvine.domain.service.extractor.DocumentExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PdfDocumentExtractor implements DocumentExtractor {

    @Override
    public boolean supports(IngestionDocumentType type) {
        return type == IngestionDocumentType.PDF;
    }

    @Override
    public ExtractedDocument extractText(byte[] document, String mimeType) {
        try (PDDocument pdf = Loader.loadPDF(document)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(pdf);

            int pageCount = pdf.getNumberOfPages();
            List<DocumentSection> sections = new ArrayList<>();

            PDFTextStripper pageStripper = new PDFTextStripper();
            for (int page = 1; page <= pageCount; page++) {
                pageStripper.setStartPage(page);
                pageStripper.setEndPage(page);

                String pageText = pageStripper.getText(pdf).trim();

                sections.add(new DocumentSection(
                        "page-" + page,
                        "Page " + page,
                        page - 1,
                        pageText
                ));
            }

            return new ExtractedDocument(
                    fullText,
                    sections,
                    Map.of(
                            "mimeType", mimeType,
                            "pages", pageCount
                    )
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse PDF", e);
        }
    }
}
