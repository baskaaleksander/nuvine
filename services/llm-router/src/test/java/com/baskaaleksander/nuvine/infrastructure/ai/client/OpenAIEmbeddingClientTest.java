package com.baskaaleksander.nuvine.infrastructure.ai.client;

import com.baskaaleksander.nuvine.application.dto.EmbeddingApiResponse;
import com.baskaaleksander.nuvine.infrastructure.config.OpenAIConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAIEmbeddingClientTest {

    @Mock
    private RestTemplate openAiRestTemplate;

    @Mock
    private OpenAIConfig.OpenAIProperties props;

    @InjectMocks
    private OpenAIEmbeddingClient openAIEmbeddingClient;

    private List<Float> embedding1;
    private List<Float> embedding2;

    @BeforeEach
    void setUp() {
        embedding1 = List.of(0.1f, 0.2f, 0.3f);
        embedding2 = List.of(0.4f, 0.5f, 0.6f);
    }

    @Test
    void embed_validTexts_returnsEmbeddings() {
        List<String> texts = List.of("text1", "text2");
        
        EmbeddingApiResponse.EmbeddingData data1 = new EmbeddingApiResponse.EmbeddingData(0, embedding1);
        EmbeddingApiResponse.EmbeddingData data2 = new EmbeddingApiResponse.EmbeddingData(1, embedding2);
        EmbeddingApiResponse response = new EmbeddingApiResponse("text-embedding-3-small", List.of(data1, data2));

        when(props.baseUrl()).thenReturn("https://api.openai.com/v1");
        when(props.embeddingModel()).thenReturn("text-embedding-3-small");
        when(openAiRestTemplate.postForObject(eq("https://api.openai.com/v1/embeddings"), any(), eq(EmbeddingApiResponse.class)))
                .thenReturn(response);

        List<List<Float>> result = openAIEmbeddingClient.embed(texts);

        assertEquals(2, result.size());
        assertEquals(embedding1, result.get(0));
        assertEquals(embedding2, result.get(1));
        verify(openAiRestTemplate).postForObject(eq("https://api.openai.com/v1/embeddings"), any(), eq(EmbeddingApiResponse.class));
    }

    @Test
    void embed_sortsResponseByIndex() {
        List<String> texts = List.of("text1", "text2");
        
        // Response comes back out of order (index 1 first, then index 0)
        EmbeddingApiResponse.EmbeddingData data1 = new EmbeddingApiResponse.EmbeddingData(1, embedding2);
        EmbeddingApiResponse.EmbeddingData data2 = new EmbeddingApiResponse.EmbeddingData(0, embedding1);
        EmbeddingApiResponse response = new EmbeddingApiResponse("text-embedding-3-small", List.of(data1, data2));

        when(props.baseUrl()).thenReturn("https://api.openai.com/v1");
        when(props.embeddingModel()).thenReturn("text-embedding-3-small");
        when(openAiRestTemplate.postForObject(eq("https://api.openai.com/v1/embeddings"), any(), eq(EmbeddingApiResponse.class)))
                .thenReturn(response);

        List<List<Float>> result = openAIEmbeddingClient.embed(texts);

        // Result should be sorted by index: index 0 first, then index 1
        assertEquals(embedding1, result.get(0));
        assertEquals(embedding2, result.get(1));
    }

    @Test
    void embed_nullResponse_throwsIllegalStateException() {
        List<String> texts = List.of("text1");

        when(props.baseUrl()).thenReturn("https://api.openai.com/v1");
        when(props.embeddingModel()).thenReturn("text-embedding-3-small");
        when(openAiRestTemplate.postForObject(anyString(), any(), eq(EmbeddingApiResponse.class)))
                .thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> openAIEmbeddingClient.embed(texts));
        
        assertEquals("Empty response from OpenAI embeddings API", exception.getMessage());
    }

    @Test
    void embed_emptyTexts_returnsEmptyList() {
        List<List<Float>> resultEmpty = openAIEmbeddingClient.embed(List.of());
        List<List<Float>> resultNull = openAIEmbeddingClient.embed(null);

        assertTrue(resultEmpty.isEmpty());
        assertTrue(resultNull.isEmpty());
        verifyNoInteractions(openAiRestTemplate);
    }
}
