package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.service.CompletionService;
import com.baskaaleksander.nuvine.domain.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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
        return ResponseEntity.ok(
                completionService.call(request.model(), request.message(), request.messages())
        );
    }

    @PostMapping(
            value = "/completion/stream",
            produces = MediaType.APPLICATION_NDJSON_VALUE
    )
    public Flux<LlmChunk> completionStream(@RequestBody CompletionRequest request) {
        return completionService.callStream(request.model(), request.message(), request.messages());
    }
}