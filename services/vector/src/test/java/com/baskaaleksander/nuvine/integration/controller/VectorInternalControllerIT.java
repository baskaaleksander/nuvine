package com.baskaaleksander.nuvine.integration.controller;

import com.baskaaleksander.nuvine.application.dto.TextVectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchResponse;
import com.baskaaleksander.nuvine.integration.base.BaseControllerIntegrationTest;
import com.baskaaleksander.nuvine.integration.support.JwtTestUtils;
import com.baskaaleksander.nuvine.integration.support.WireMockStubs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VectorInternalControllerIT extends BaseControllerIntegrationTest {

    private JwtTestUtils jwtTestUtils;
    private WireMockStubs wireMockStubs;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtTestUtils = new JwtTestUtils("http://localhost:" + wireMockServer.port() + "/realms/nuvine");
        wireMockStubs = new WireMockStubs(wireMockServer);
        userId = UUID.randomUUID();
    }

    @Test
    void searchByText_withInternalServiceRole_shouldReturn200() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        List<Float> mockEmbedding = generateMockEmbedding(1536);
        wireMockStubs.stubLlmRouterEmbeddings(mockEmbedding);

        String jwt = jwtTestUtils.generateJwt(userId, "service@test.com", List.of("ROLE_INTERNAL_SERVICE"));

        TextVectorSearchRequest request = new TextVectorSearchRequest(
                workspaceId,
                projectId,
                List.of(documentId),
                "test query",
                10,
                0.7f
        );

        HttpHeaders headers = authHeaders(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<VectorSearchResponse> response = restTemplate.exchange(
                "/api/v1/internal/vector/search-by-text",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                VectorSearchResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().matches()).isNotNull();
    }

    @Test
    void search_withInternalServiceRole_shouldReturn200() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        String jwt = jwtTestUtils.generateJwt(userId, "service@test.com", List.of("ROLE_INTERNAL_SERVICE"));

        List<Float> queryVector = generateMockEmbedding(1536);

        VectorSearchRequest request = new VectorSearchRequest(
                workspaceId,
                projectId,
                List.of(documentId),
                queryVector,
                10,
                0.7f
        );

        HttpHeaders headers = authHeaders(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<VectorSearchResponse> response = restTemplate.exchange(
                "/api/v1/internal/vector/search",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                VectorSearchResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().matches()).isNotNull();
    }

    @Test
    void searchByText_withoutInternalServiceRole_shouldReturn403() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        String jwt = jwtTestUtils.generateJwt(userId, "user@test.com", List.of("ROLE_USER"));

        TextVectorSearchRequest request = new TextVectorSearchRequest(
                workspaceId,
                projectId,
                List.of(documentId),
                "test query",
                10,
                0.7f
        );

        HttpHeaders headers = authHeaders(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/internal/vector/search-by-text",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void search_withoutAuthentication_shouldReturn401() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        VectorSearchRequest request = new VectorSearchRequest(
                workspaceId,
                projectId,
                List.of(documentId),
                generateMockEmbedding(1536),
                10,
                0.7f
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/internal/vector/search",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void searchByText_withExpiredToken_shouldReturn401() {
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        String expiredJwt = jwtTestUtils.generateExpiredJwt(userId, "service@test.com");

        TextVectorSearchRequest request = new TextVectorSearchRequest(
                workspaceId,
                projectId,
                List.of(documentId),
                "test query",
                10,
                0.7f
        );

        HttpHeaders headers = authHeaders(expiredJwt);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/internal/vector/search-by-text",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private List<Float> generateMockEmbedding(int dimensions) {
        List<Float> embedding = new ArrayList<>();
        for (int i = 0; i < dimensions; i++) {
            embedding.add((float) Math.random());
        }
        return embedding;
    }
}
