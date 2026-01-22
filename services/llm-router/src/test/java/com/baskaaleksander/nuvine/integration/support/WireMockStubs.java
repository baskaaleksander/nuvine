package com.baskaaleksander.nuvine.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WireMockStubs {

    private final WireMockServer wireMockServer;
    private static final Random RANDOM = new Random(42);

    public WireMockStubs(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    // ==================== OpenAI Embeddings Stubs ====================

    public void stubOpenAIEmbedding(String model, List<List<Float>> embeddings) {
        StringBuilder dataJson = new StringBuilder("[");
        for (int i = 0; i < embeddings.size(); i++) {
            if (i > 0) dataJson.append(",");
            dataJson.append(String.format("""
                {
                    "object": "embedding",
                    "index": %d,
                    "embedding": %s
                }
                """, i, embeddingToJson(embeddings.get(i))));
        }
        dataJson.append("]");

        String responseJson = String.format("""
            {
                "object": "list",
                "model": "%s",
                "data": %s,
                "usage": {
                    "prompt_tokens": 10,
                    "total_tokens": 10
                }
            }
            """, model, dataJson);

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/embeddings"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseJson))
        );
    }

    public void stubOpenAIEmbeddingSingle(String model, List<Float> embedding) {
        stubOpenAIEmbedding(model, List.of(embedding));
    }

    public void stubOpenAIRateLimited(int retryAfterSeconds) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/embeddings"))
                .willReturn(WireMock.aResponse()
                    .withStatus(429)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Retry-After", String.valueOf(retryAfterSeconds))
                    .withBody("""
                        {
                            "error": {
                                "message": "Rate limit exceeded",
                                "type": "rate_limit_error",
                                "code": "rate_limit_exceeded"
                            }
                        }
                        """))
        );
    }

    public void stubOpenAIError(int status, String message) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/embeddings"))
                .willReturn(WireMock.aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody(String.format("""
                        {
                            "error": {
                                "message": "%s",
                                "type": "api_error",
                                "code": "internal_error"
                            }
                        }
                        """, message)))
        );
    }

    // ==================== OpenRouter Completion Stubs ====================

    public void stubOpenRouterCompletion(String model, String content, int tokensIn, int tokensOut) {
        String responseJson = String.format("""
            {
                "id": "chatcmpl-test-123",
                "object": "chat.completion",
                "created": 1700000000,
                "model": "%s",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "%s"
                        },
                        "finish_reason": "stop"
                    }
                ],
                "usage": {
                    "prompt_tokens": %d,
                    "completion_tokens": %d,
                    "total_tokens": %d
                }
            }
            """, model, escapeJson(content), tokensIn, tokensOut, tokensIn + tokensOut);

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/chat/completions"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseJson))
        );
    }

    public void stubOpenRouterStreamingCompletion(String model, List<String> contentChunks) {
        StringBuilder sseResponse = new StringBuilder();

        for (int i = 0; i < contentChunks.size(); i++) {
            String chunk = contentChunks.get(i);
            String chunkJson = String.format("""
                {"id":"chatcmpl-test-123","object":"chat.completion.chunk","created":1700000000,"model":"%s","choices":[{"index":0,"delta":{"content":"%s"},"finish_reason":null}]}
                """, model, escapeJson(chunk));
            sseResponse.append("data: ").append(chunkJson.trim()).append("\n\n");
        }

        // Final chunk with finish_reason
        String finalChunk = String.format("""
            {"id":"chatcmpl-test-123","object":"chat.completion.chunk","created":1700000000,"model":"%s","choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":%d,"total_tokens":%d}}
            """, model, contentChunks.size() * 2, 10 + contentChunks.size() * 2);
        sseResponse.append("data: ").append(finalChunk.trim()).append("\n\n");
        sseResponse.append("data: [DONE]\n\n");

        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/chat/completions"))
                .withRequestBody(WireMock.containing("\"stream\":true"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/event-stream")
                    .withBody(sseResponse.toString()))
        );
    }

    public void stubOpenRouterError(int status, String message) {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/chat/completions"))
                .willReturn(WireMock.aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody(String.format("""
                        {
                            "error": {
                                "message": "%s",
                                "type": "api_error",
                                "code": "internal_error"
                            }
                        }
                        """, message)))
        );
    }

    public void stubOpenRouterRateLimited() {
        wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/chat/completions"))
                .willReturn(WireMock.aResponse()
                    .withStatus(429)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Retry-After", "60")
                    .withBody("""
                        {
                            "error": {
                                "message": "Rate limit exceeded",
                                "type": "rate_limit_error",
                                "code": "rate_limit_exceeded"
                            }
                        }
                        """))
        );
    }

    // ==================== Keycloak Stubs ====================

    public void stubKeycloakJwks() {
        String jwksJson = String.format("""
            {
                "keys": [
                    {
                        "kty": "RSA",
                        "use": "sig",
                        "alg": "RS256",
                        "kid": "%s",
                        "n": "%s",
                        "e": "%s"
                    }
                ]
            }
            """,
            JwtTestUtils.getKeyId(),
            JwtTestUtils.getModulusBase64Url(),
            JwtTestUtils.getExponentBase64Url()
        );

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/realms/nuvine/protocol/openid-connect/certs"))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(jwksJson))
        );
    }

    // ==================== Helper Methods ====================

    public static List<Float> generateTestEmbedding(int dimension) {
        return IntStream.range(0, dimension)
                .mapToObj(i -> RANDOM.nextFloat() * 2 - 1)
                .collect(Collectors.toList());
    }

    public static List<Float> generateTestEmbedding() {
        return generateTestEmbedding(1536);
    }

    private String embeddingToJson(List<Float> embedding) {
        return "[" + embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
