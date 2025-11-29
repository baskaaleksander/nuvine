package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.CompletionRequest;
import com.baskaaleksander.nuvine.domain.exception.ContextNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagPromptBuilder {

    private final ContextRetrievalService contextRetrievalService;

    public String buildPrompt(CompletionRequest request, List<UUID> documentIds) {
        if (request.freeMode()) {
            log.info(
                    "RAG_PROMPT_BUILD FREE_MODE workspaceId={} projectId={}",
                    request.workspaceId(),
                    request.projectId()
            );
            return request.message();
        }

        log.info(
                "RAG_PROMPT_BUILD START workspaceId={} projectId={} strictMode={}",
                request.workspaceId(),
                request.projectId(),
                request.strictMode()
        );

        List<String> context = contextRetrievalService.retrieveContext(
                request.workspaceId(),
                request.projectId(),
                documentIds,
                request.message(),
                10,
                0.5f
        );

        int contextSize = context != null ? context.size() : 0;
        log.info(
                "RAG_PROMPT_BUILD CONTEXT_RETRIEVED workspaceId={} projectId={} contextChunks={}",
                request.workspaceId(),
                request.projectId(),
                contextSize
        );

        String prompt;
        if (request.strictMode()) {
            prompt = buildStrictPrompt(context, request.message(), request);
        } else {
            prompt = buildNonStrictPrompt(context, request.message(), request);
        }

        log.info(
                "RAG_PROMPT_BUILD END workspaceId={} projectId={} strictMode={} contextChunks={}",
                request.workspaceId(),
                request.projectId(),
                request.strictMode(),
                contextSize
        );

        return prompt;
    }

    private String buildNonStrictPrompt(List<String> context, String userMessage, CompletionRequest request) {
        String ragPart = (context != null && !context.isEmpty())
                ? "Use the following context extracted from knowledge base to answer the user:\n---\n"
                + String.join("\n", context)
                + "\n---\n"
                : "No relevant context was found. Answer based only on your general knowledge.\n";

        if (context == null || context.isEmpty()) {
            log.info(
                    "RAG_PROMPT_BUILD NON_STRICT_NO_CONTEXT workspaceId={} projectId={}",
                    request.workspaceId(),
                    request.projectId()
            );
        }

        return ragPart + "User message:\n" + userMessage;
    }

    private String buildStrictPrompt(List<String> context, String userMessage, CompletionRequest request) {
        if (context == null || context.isEmpty()) {
            log.info(
                    "RAG_PROMPT_BUILD STRICT_NO_CONTEXT workspaceId={} projectId={}",
                    request.workspaceId(),
                    request.projectId()
            );
            throw new ContextNotFoundException("Context not found");
        }

        String formattedContext = String.join("\n\n---\n\n", context);

        String systemPrompt = """
                You are an AI assistant that must answer strictly and only using the context from the user's documents.
                
                Rules:
                - Use only the information contained inside <context>...</context>.
                - If the answer is not clearly supported by the context, reply that the documents do not contain enough information to answer the question.
                - Do not use any outside or general knowledge.
                - Do not invent or guess any facts, numbers, or details.
                - If the context is only partially relevant, answer only what is directly supported and explicitly say what is missing.
                - Answer in the same language as the user message.
                
                <context>
                %s
                </context>
                """.formatted(formattedContext);

        log.info(
                "RAG_PROMPT_BUILD STRICT_CONTEXT_OK workspaceId={} projectId={} contextChunks={}",
                request.workspaceId(),
                request.projectId(),
                context.size()
        );

        return systemPrompt + "\nUser message:\n" + userMessage;
    }
}