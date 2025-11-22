package com.baskaaleksander.nuvine.domain.model;

public record DocumentSection(
        String id,
        String title,
        int order,
        String content
) {
}
