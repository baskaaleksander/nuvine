package com.baskaaleksander.nuvine.domain.service.extractor.impl;

import com.baskaaleksander.nuvine.domain.model.DocumentSection;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;
import com.baskaaleksander.nuvine.domain.service.extractor.DocumentExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PptxDocumentExtractor implements DocumentExtractor {

    @Override
    public boolean supports(IngestionDocumentType type) {
        return type == IngestionDocumentType.PPTX;
    }

    @Override
    public ExtractedDocument extractText(byte[] document, String mimeType) {
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(document))) {

            List<XSLFSlide> slides = ppt.getSlides();
            List<DocumentSection> sections = new ArrayList<>();
            StringBuilder global = new StringBuilder();

            int index = 0;
            for (XSLFSlide slide : slides) {
                index++;

                String title = "Slide " + index;
                StringBuilder content = new StringBuilder();
                boolean titleTaken = false;

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String txt = textShape.getText();
                        if (txt != null && !txt.isBlank()) {
                            if (!titleTaken) {
                                title = txt.trim();
                                titleTaken = true;
                            } else {
                                content.append(txt).append("\n");
                            }
                        }
                    }
                }

                String slideText = content.toString().trim();
                global.append("[").append(title).append("]\n")
                        .append(slideText)
                        .append("\n\n");

                sections.add(new DocumentSection(
                        "slide-" + index,
                        title,
                        index - 1,
                        slideText
                ));
            }

            return new ExtractedDocument(
                    global.toString().trim(),
                    sections,
                    Map.of(
                            "mimeType", mimeType,
                            "slides", slides.size()
                    )
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PPTX", e);
        }
    }
}
