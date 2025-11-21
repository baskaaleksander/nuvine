package com.baskaaleksander.nuvine.domain.model;

public enum IngestionStage {
    QUEUED, FETCH, OCR, PARSE, CHUNK, EMBED, GRAPH
}
