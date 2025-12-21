package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.LlmChunk;
import com.baskaaleksander.nuvine.application.dto.OpenRouterChatStreamRequest;
import com.baskaaleksander.nuvine.application.dto.OpenRouterStreamEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenRouterStreamService {

    private final WebClient openRouterWebClient;
    private final ObjectMapper objectMapper;

    public Flux<LlmChunk> stream(OpenRouterChatStreamRequest request) {
        OpenRouterChatStreamRequest streamingRequest = new OpenRouterChatStreamRequest(
                request.model(),
                request.messages(),
                request.temperature(),
                request.maxTokens(),
                true,
                request.streamOptions()
        );

        return openRouterWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(streamingRequest)
                .retrieve()
                .bodyToFlux(String.class)
                .handle(this::handleSseData);
    }

    private void handleSseData(String data, SynchronousSink<LlmChunk> sink) {
        if (data == null || data.isBlank()) {
            return;
        }

        log.info(data);

        String cleanData = data;
        if (data.startsWith("data:")) {
            cleanData = data.substring(5);
        }


        if ("[DONE]".equals(cleanData)) {
            sink.next(new LlmChunk("done", null, null, null, null));
            return;
        }

        try {
            OpenRouterStreamEvent event =
                    objectMapper.readValue(cleanData, OpenRouterStreamEvent.class);

            if (event.usage() != null) {
                log.info("STREAM_USAGE emitted: {}", event.usage());
                sink.next(new LlmChunk(
                        "usage",
                        null,
                        event.usage().promptTokens(),
                        event.usage().completionTokens(),
                        event.model()
                ));
            }

            OpenRouterStreamEvent.Choice choice =
                    (event.choices() == null || event.choices().isEmpty())
                            ? null
                            : event.choices().getFirst();

            if (choice != null && choice.delta() != null) {
                String content = choice.delta().content();

                if (content != null && !content.isEmpty()) {
                    sink.next(new LlmChunk(
                            "delta",
                            content,
                            null,
                            null,
                            event.model()
                    ));
                }
            }


        } catch (Exception e) {
            log.error("Failed to parse stream data: '{}'", cleanData, e);
            sink.next(new LlmChunk("error", null, null, null, null));
        }
    }
}