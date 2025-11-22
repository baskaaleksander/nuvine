package com.baskaaleksander.nuvine.domain.service.extractor;

import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.IngestionDocumentType;

public interface DocumentExtractor {
    boolean supports(IngestionDocumentType type);

    ExtractedDocument extractText(byte[] document, String mimeType);
}
