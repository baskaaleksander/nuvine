package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CompletionRequest;
import com.baskaaleksander.nuvine.domain.exception.ContextNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagPromptBuilderTest {

    @Mock
    private ContextRetrievalService contextRetrievalService;

    @InjectMocks
    private RagPromptBuilder ragPromptBuilder;

    private UUID workspaceId;
    private UUID projectId;
    private List<UUID> documentIds;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        documentIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    }

    private CompletionRequest createRequest(String message, boolean freeMode, boolean strictMode) {
        return new CompletionRequest(
                workspaceId,
                projectId,
                documentIds,
                UUID.randomUUID(),
                message,
                "gpt-4",
                5,
                strictMode,
                freeMode
        );
    }

    @Nested
    @DisplayName("buildPrompt - Free Mode")
    class FreeModeTests {

        @Test
        @DisplayName("Should return raw message without context retrieval when freeMode is true")
        void buildPrompt_freeMode_returnsRawMessage() {
            String userMessage = "Tell me about wine tasting";
            CompletionRequest request = createRequest(userMessage, true, false);

            String result = ragPromptBuilder.buildPrompt(request, documentIds);

            assertThat(result).isEqualTo(userMessage);
            verify(contextRetrievalService, never()).retrieveContext(
                    any(), any(), any(), any(), anyInt(), anyFloat()
            );
        }

        @Test
        @DisplayName("Should return raw message in free mode even when strictMode flag is also true")
        void buildPrompt_freeModeWithStrictFlag_returnsRawMessage() {
            String userMessage = "What are tannins?";
            CompletionRequest request = createRequest(userMessage, true, true);

            String result = ragPromptBuilder.buildPrompt(request, documentIds);

            assertThat(result).isEqualTo(userMessage);
            verify(contextRetrievalService, never()).retrieveContext(
                    any(), any(), any(), any(), anyInt(), anyFloat()
            );
        }
    }

    @Nested
    @DisplayName("buildPrompt - Strict Mode")
    class StrictModeTests {

        @Test
        @DisplayName("Should include formatted context with system prompt when context is available")
        void buildPrompt_strictMode_withContext_includesFormattedContext() {
            String userMessage = "What temperature should I serve red wine?";
            CompletionRequest request = createRequest(userMessage, false, true);
            List<String> context = List.of(
                    "Red wine should be served at 60-68°F",
                    "Lighter reds like Pinot Noir can be served slightly cooler"
            );

            when(contextRetrievalService.retrieveContext(
                    eq(workspaceId), eq(projectId), eq(documentIds), eq(userMessage), eq(10), eq(0.5f)
            )).thenReturn(context);

            String result = ragPromptBuilder.buildPrompt(request, documentIds);

            assertThat(result)
                    .contains("You are an AI assistant that must answer strictly and only using the context")
                    .contains("<context>")
                    .contains("Red wine should be served at 60-68°F")
                    .contains("Lighter reds like Pinot Noir can be served slightly cooler")
                    .contains("</context>")
                    .contains("User message:")
                    .contains(userMessage);
        }

        @Test
        @DisplayName("Should throw ContextNotFoundException when no context is found in strict mode")
        void buildPrompt_strictMode_noContext_throwsContextNotFoundException() {
            String userMessage = "What is the meaning of life?";
            CompletionRequest request = createRequest(userMessage, false, true);

            when(contextRetrievalService.retrieveContext(
                    eq(workspaceId), eq(projectId), eq(documentIds), eq(userMessage), eq(10), eq(0.5f)
            )).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> ragPromptBuilder.buildPrompt(request, documentIds))
                    .isInstanceOf(ContextNotFoundException.class)
                    .hasMessage("Context not found");
        }

        @Test
        @DisplayName("Should throw ContextNotFoundException when context is null in strict mode")
        void buildPrompt_strictMode_nullContext_throwsContextNotFoundException() {
            String userMessage = "Random question";
            CompletionRequest request = createRequest(userMessage, false, true);

            when(contextRetrievalService.retrieveContext(
                    eq(workspaceId), eq(projectId), eq(documentIds), eq(userMessage), eq(10), eq(0.5f)
            )).thenReturn(null);

            assertThatThrownBy(() -> ragPromptBuilder.buildPrompt(request, documentIds))
                    .isInstanceOf(ContextNotFoundException.class)
                    .hasMessage("Context not found");
        }
    }

    @Nested
    @DisplayName("buildPrompt - Regular Mode (Non-Strict)")
    class RegularModeTests {

        @Test
        @DisplayName("Should include context when available in regular mode")
        void buildPrompt_regularMode_withContext_includesContext() {
            String userMessage = "How do I pair wine with cheese?";
            CompletionRequest request = createRequest(userMessage, false, false);
            List<String> context = List.of(
                    "Pair bold red wines with aged cheeses",
                    "White wines complement soft cheeses well"
            );

            when(contextRetrievalService.retrieveContext(
                    eq(workspaceId), eq(projectId), eq(documentIds), eq(userMessage), eq(10), eq(0.5f)
            )).thenReturn(context);

            String result = ragPromptBuilder.buildPrompt(request, documentIds);

            assertThat(result)
                    .contains("Use the following context extracted from knowledge base")
                    .contains("Pair bold red wines with aged cheeses")
                    .contains("White wines complement soft cheeses well")
                    .contains("User message:")
                    .contains(userMessage);
        }

        @Test
        @DisplayName("Should return fallback prompt when no context is found in regular mode")
        void buildPrompt_regularMode_noContext_returnsFallbackPrompt() {
            String userMessage = "What is quantum physics?";
            CompletionRequest request = createRequest(userMessage, false, false);

            when(contextRetrievalService.retrieveContext(
                    eq(workspaceId), eq(projectId), eq(documentIds), eq(userMessage), eq(10), eq(0.5f)
            )).thenReturn(Collections.emptyList());

            String result = ragPromptBuilder.buildPrompt(request, documentIds);

            assertThat(result)
                    .contains("No relevant context was found. Answer based only on your general knowledge.")
                    .contains("User message:")
                    .contains(userMessage);
        }

        @Test
        @DisplayName("Should return fallback prompt when context is null in regular mode")
        void buildPrompt_regularMode_nullContext_returnsFallbackPrompt() {
            String userMessage = "Tell me something";
            CompletionRequest request = createRequest(userMessage, false, false);

            when(contextRetrievalService.retrieveContext(
                    eq(workspaceId), eq(projectId), eq(documentIds), eq(userMessage), eq(10), eq(0.5f)
            )).thenReturn(null);

            String result = ragPromptBuilder.buildPrompt(request, documentIds);

            assertThat(result)
                    .contains("No relevant context was found. Answer based only on your general knowledge.")
                    .contains("User message:")
                    .contains(userMessage);
        }
    }

    @Nested
    @DisplayName("buildPrompt - Context Retrieval Parameters")
    class ContextRetrievalParameterTests {

        @Test
        @DisplayName("Should call context retrieval with correct parameters")
        void buildPrompt_callsContextRetrievalWithCorrectParams() {
            String userMessage = "Test message";
            CompletionRequest request = createRequest(userMessage, false, false);

            when(contextRetrievalService.retrieveContext(
                    any(), any(), any(), any(), anyInt(), anyFloat()
            )).thenReturn(List.of("Some context"));

            ragPromptBuilder.buildPrompt(request, documentIds);

            verify(contextRetrievalService).retrieveContext(
                    workspaceId,
                    projectId,
                    documentIds,
                    userMessage,
                    10,
                    0.5f
            );
        }
    }
}
