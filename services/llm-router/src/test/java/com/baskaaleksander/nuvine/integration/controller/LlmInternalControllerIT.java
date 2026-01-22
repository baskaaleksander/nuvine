package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.CompletionRequest;
import com.baskaaleksander.nuvine.application.dto.CompletionResponse;
import com.baskaaleksander.nuvine.application.dto.EmbeddingRequest;
import com.baskaaleksander.nuvine.application.dto.EmbeddingResponse;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LlmInternalControllerIT extends BaseControllerIntegrationTest {

    private static final String EMBEDDINGS_URL = "/api/v1/internal/llm/embeddings";
    private static final String COMPLETIONS_URL = "/api/v1/internal/llm/completions";
    private static final String STREAM_URL = "/api/v1/internal/llm/completion/stream";

    private JwtTestUtils jwtTestUtils;
    private WireMockStubs wireMockStubs;

    @BeforeEach
    void setup() {
        jwtTestUtils = new JwtTestUtils("http://localhost:" + wireMockServer.port() + "/realms/nuvine");
        wireMockStubs = new WireMockStubs(wireMockServer);
    }

    @Nested
    @DisplayName("POST /api/v1/internal/llm/embeddings")
    class EmbeddingsEndpointTests {

        @Test
        @DisplayName("Should create embedding for single text with INTERNAL_SERVICE role")
        void shouldCreateEmbeddingForSingleText() {
            // Given
            String internalServiceJwt = jwtTestUtils.generateInternalServiceJwt();
            List<Float> testEmbedding = WireMockStubs.generateTestEmbedding();
            wireMockStubs.stubOpenAIEmbeddingSingle("text-embedding-3-small", testEmbedding);

            EmbeddingRequest request = new EmbeddingRequest(
                    List.of("Test text for embedding"),
                    "text-embedding-3-small"
            );

            HttpHeaders headers = authHeaders(internalServiceJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
                    EMBEDDINGS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    EmbeddingResponse.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().embeddings()).hasSize(1);
            assertThat(response.getBody().embeddings().getFirst()).hasSize(1536);
            assertThat(response.getBody().usedModel()).isEqualTo("text-embedding-3-small");
        }

        @Test
        @DisplayName("Should create embeddings for batch of texts with INTERNAL_SERVICE role")
        void shouldCreateEmbeddingsForBatchTexts() {
            // Given
            String internalServiceJwt = jwtTestUtils.generateInternalServiceJwt();
            List<List<Float>> testEmbeddings = List.of(
                    WireMockStubs.generateTestEmbedding(),
                    WireMockStubs.generateTestEmbedding(),
                    WireMockStubs.generateTestEmbedding()
            );
            wireMockStubs.stubOpenAIEmbedding("text-embedding-3-small", testEmbeddings);

            EmbeddingRequest request = new EmbeddingRequest(
                    List.of("Text one", "Text two", "Text three"),
                    "text-embedding-3-small"
            );

            HttpHeaders headers = authHeaders(internalServiceJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
                    EMBEDDINGS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    EmbeddingResponse.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().embeddings()).hasSize(3);
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuth() {
            // Given
            EmbeddingRequest request = new EmbeddingRequest(
                    List.of("Test text"),
                    "text-embedding-3-small"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    EMBEDDINGS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 403 with USER role (requires INTERNAL_SERVICE)")
        void shouldReturn403WithUserRole() {
            // Given
            String userJwt = jwtTestUtils.generateUserJwt(UUID.randomUUID(), "user@test.com");
            EmbeddingRequest request = new EmbeddingRequest(
                    List.of("Test text"),
                    "text-embedding-3-small"
            );

            HttpHeaders headers = authHeaders(userJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    EMBEDDINGS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should handle OpenAI rate limit error")
        void shouldHandleRateLimitError() {
            // Given
            String internalServiceJwt = jwtTestUtils.generateInternalServiceJwt();
            wireMockStubs.stubOpenAIRateLimited(60);

            EmbeddingRequest request = new EmbeddingRequest(
                    List.of("Test text"),
                    "text-embedding-3-small"
            );

            HttpHeaders headers = authHeaders(internalServiceJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    EMBEDDINGS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/internal/llm/completions")
    class CompletionsEndpointTests {

        @Test
        @DisplayName("Should create completion with INTERNAL_SERVICE role")
        void shouldCreateCompletionSuccessfully() {
            // Given
            String internalServiceJwt = jwtTestUtils.generateInternalServiceJwt();
            wireMockStubs.stubOpenRouterCompletion(
                    "openai/gpt-4o-mini",
                    "This is a test response from the LLM.",
                    25,
                    10
            );

            CompletionRequest request = new CompletionRequest(
                    "What is the capital of France?",
                    "openai/gpt-4o-mini",
                    null
            );

            HttpHeaders headers = authHeaders(internalServiceJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<CompletionResponse> response = restTemplate.exchange(
                    COMPLETIONS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    CompletionResponse.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().content()).isEqualTo("This is a test response from the LLM.");
            assertThat(response.getBody().tokensIn()).isEqualTo(25);
            assertThat(response.getBody().tokensOut()).isEqualTo(10);
            assertThat(response.getBody().modelUsed()).isEqualTo("openai/gpt-4o-mini");
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuth() {
            // Given
            CompletionRequest request = new CompletionRequest(
                    "Test prompt",
                    "openai/gpt-4o-mini",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    COMPLETIONS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 403 with USER role (requires INTERNAL_SERVICE)")
        void shouldReturn403WithUserRole() {
            // Given
            String userJwt = jwtTestUtils.generateUserJwt(UUID.randomUUID(), "user@test.com");
            CompletionRequest request = new CompletionRequest(
                    "Test prompt",
                    "openai/gpt-4o-mini",
                    null
            );

            HttpHeaders headers = authHeaders(userJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    COMPLETIONS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Should handle OpenRouter error")
        void shouldHandleOpenRouterError() {
            // Given
            String internalServiceJwt = jwtTestUtils.generateInternalServiceJwt();
            wireMockStubs.stubOpenRouterError(500, "Internal server error");

            CompletionRequest request = new CompletionRequest(
                    "Test prompt",
                    "openai/gpt-4o-mini",
                    null
            );

            HttpHeaders headers = authHeaders(internalServiceJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    COMPLETIONS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/internal/llm/completion/stream")
    class StreamEndpointTests {

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuth() {
            // Given
            CompletionRequest request = new CompletionRequest(
                    "Test prompt",
                    "openai/gpt-4o-mini",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    STREAM_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return 403 with USER role (requires INTERNAL_SERVICE)")
        void shouldReturn403WithUserRole() {
            // Given
            String userJwt = jwtTestUtils.generateUserJwt(UUID.randomUUID(), "user@test.com");
            CompletionRequest request = new CompletionRequest(
                    "Test prompt",
                    "openai/gpt-4o-mini",
                    null
            );

            HttpHeaders headers = authHeaders(userJwt);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    STREAM_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
