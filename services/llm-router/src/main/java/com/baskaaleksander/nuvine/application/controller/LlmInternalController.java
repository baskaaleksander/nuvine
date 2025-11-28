package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.CompletionRequest;
import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.application.dto.EmbeddingRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingResponse;
import com.baskaaleksander.nuvine.domain.service.CompletionService;
import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/llm")
@RequiredArgsConstructor
public class LlmInternalController {

    private final EmbeddingService embeddingService;
    private final CompletionService completionService;

    @PostMapping("/embeddings")
    public ResponseEntity<EmbeddingResponse> getEmbeddings(
            @RequestBody EmbeddingRequest request
    ) {
        return ResponseEntity.ok(embeddingService.createEmbeddings(request));
    }

    @PostMapping("/completions")
    public ResponseEntity<CompletionResponse> completion(
            @RequestBody CompletionRequest request
    ) {
        return ResponseEntity.ok(completionService.call(request.model(), request.message()));
    }
}
