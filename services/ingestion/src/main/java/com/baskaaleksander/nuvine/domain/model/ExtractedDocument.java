package com.baskaaleksander.nuvine.domain.model;

import java.util.List;
import java.util.Map;

public record ExtractedDocument(
        String text,
        List<DocumentSection> sections,
        Map<String, Object> metadata
) {
}
