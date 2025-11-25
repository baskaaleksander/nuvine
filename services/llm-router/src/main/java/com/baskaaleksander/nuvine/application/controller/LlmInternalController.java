package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.EmbeddingsRequest;
import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/llm")
@RequiredArgsConstructor
public class LlmInternalController {

    private final EmbeddingService embeddingService;

    @PostMapping("/embeddings")
    public ResponseEntity<List<Float>> getEmbeddings(
            @RequestBody EmbeddingsRequest request
    ) {
        return ResponseEntity.ok(embeddingService.createEmbedding(request.input()));
    }
}
