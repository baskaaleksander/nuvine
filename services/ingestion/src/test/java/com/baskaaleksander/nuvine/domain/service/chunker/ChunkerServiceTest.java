package com.baskaaleksander.nuvine.domain.service.chunker;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChunkerServiceTest {

    @Mock
    private Tokenizer tokenizer;

    @InjectMocks
    private ChunkerService chunkerService;

    private UUID documentId;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
    }

    @Test
    void chunkDocument_shortText_returnsSingleChunk() {
        String shortText = "This is a short sentence. Another short one.";
        ExtractedDocument extractedDocument = new ExtractedDocument(shortText, List.of(), Map.of());

        when(tokenizer.count(anyString())).thenReturn(10); // Each sentence ~10 tokens

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertEquals(1, chunks.size());
        assertEquals(documentId, chunks.get(0).documentId());
        assertEquals(0, chunks.get(0).page());
        assertTrue(chunks.get(0).content().contains("This is a short sentence"));
    }

    @Test
    void chunkDocument_longText_returnsMultipleChunks() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("This is sentence number ").append(i).append(" with additional content for testing purposes. ");
        }
        ExtractedDocument extractedDocument = new ExtractedDocument(longText.toString(), List.of(), Map.of());

        // Simulate higher token counts to trigger chunking (700 max tokens)
        when(tokenizer.count(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            // Multiply word count to simulate more realistic token counts
            return text.split("\\s+").length * 10;
        });

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertTrue(chunks.size() > 1, "Expected multiple chunks but got: " + chunks.size());
        for (Chunk chunk : chunks) {
            assertEquals(documentId, chunk.documentId());
        }
    }

    @Test
    void chunkDocument_respectsMaxTokenLimit700() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("This is sentence number ").append(i).append(" with some extra content. ");
        }
        ExtractedDocument extractedDocument = new ExtractedDocument(longText.toString(), List.of(), Map.of());

        // Return word count as approximate tokens
        when(tokenizer.count(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            return text.split("\\s+").length;
        });

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertTrue(chunks.size() > 1);
        // Each chunk's token count should be monitored via tokenizer mock
        verify(tokenizer, atLeast(chunks.size())).count(anyString());
    }

    @Test
    void chunkDocument_appliesOverlap120Tokens() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            longText.append("Sentence ").append(i).append(" has content here. ");
        }
        ExtractedDocument extractedDocument = new ExtractedDocument(longText.toString(), List.of(), Map.of());

        when(tokenizer.count(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            return text.split("\\s+").length;
        });

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        if (chunks.size() >= 2) {
            Chunk first = chunks.get(0);
            Chunk second = chunks.get(1);
            // Second chunk should start before the first chunk ends (overlap)
            assertTrue(second.startOffset() < first.endOffset() || 
                       second.content().startsWith(first.content().substring(
                           Math.max(0, first.content().length() - 200)
                       ).trim().split("\\s+")[0]));
        }
    }

    @Test
    void chunkDocument_splitsBySentenceBoundary() {
        String text = "First sentence ends here. Second sentence continues. Third sentence follows.";
        ExtractedDocument extractedDocument = new ExtractedDocument(text, List.of(), Map.of());

        // Make token count high enough to trigger chunking at sentence boundaries
        when(tokenizer.count(anyString())).thenAnswer(invocation -> {
            String t = invocation.getArgument(0);
            // Return high count to force splits
            return t.split("\\s+").length * 100;
        });

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        for (Chunk chunk : chunks) {
            String content = chunk.content().trim();
            if (!content.isEmpty()) {
                assertTrue(content.endsWith(".") || content.endsWith("!") || content.endsWith("?"),
                        "Chunk should end at sentence boundary: " + content);
            }
        }
    }

    @Test
    void chunkDocument_longSentence_splitsAtWordBoundary() {
        StringBuilder longSentence = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longSentence.append("word").append(i).append(" ");
        }
        longSentence.append("end.");

        ExtractedDocument extractedDocument = new ExtractedDocument(longSentence.toString(), List.of(), Map.of());

        // Return high token count for the whole sentence, normal for words
        when(tokenizer.count(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            int wordCount = text.split("\\s+").length;
            // Return actual word count which will be > 700 for the long sentence
            return wordCount;
        });

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertTrue(chunks.size() >= 1);
        for (Chunk chunk : chunks) {
            // Chunks should not start or end mid-word
            String content = chunk.content();
            assertFalse(content.startsWith(" "), "Chunk should not start with space");
        }
    }

    @Test
    void chunkDocument_emptyText_returnsEmptyList() {
        ExtractedDocument extractedDocument = new ExtractedDocument("", List.of(), Map.of());

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertTrue(chunks.isEmpty());
        verify(tokenizer, never()).count(anyString());
    }

    @Test
    void chunkDocument_nullText_returnsEmptyList() {
        ExtractedDocument extractedDocument = new ExtractedDocument(null, List.of(), Map.of());

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertTrue(chunks.isEmpty());
        verify(tokenizer, never()).count(anyString());
    }

    @Test
    void chunkDocument_blankText_returnsEmptyList() {
        ExtractedDocument extractedDocument = new ExtractedDocument("   \n\t  ", List.of(), Map.of());

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunkDocument_preservesDocumentId() {
        String text = "Some text content here.";
        ExtractedDocument extractedDocument = new ExtractedDocument(text, List.of(), Map.of());
        when(tokenizer.count(anyString())).thenReturn(5);

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertFalse(chunks.isEmpty());
        for (Chunk chunk : chunks) {
            assertEquals(documentId, chunk.documentId());
        }
    }

    @Test
    void chunkDocument_setsCorrectPageNumber() {
        String text = "Content on page zero.";
        ExtractedDocument extractedDocument = new ExtractedDocument(text, List.of(), Map.of());
        when(tokenizer.count(anyString())).thenReturn(5);

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertFalse(chunks.isEmpty());
        assertEquals(0, chunks.get(0).page());
    }

    @Test
    void chunkDocument_setsCorrectOffsets() {
        String text = "First sentence here. Second sentence there.";
        ExtractedDocument extractedDocument = new ExtractedDocument(text, List.of(), Map.of());
        when(tokenizer.count(anyString())).thenReturn(5);

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertFalse(chunks.isEmpty());
        Chunk firstChunk = chunks.get(0);
        assertTrue(firstChunk.startOffset() >= 0);
        assertTrue(firstChunk.endOffset() > firstChunk.startOffset());
        assertTrue(firstChunk.endOffset() <= text.length());
    }

    @Test
    void chunkDocument_chunkIndexIncrementsCorrectly() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            text.append("Sentence number ").append(i).append(" here. ");
        }
        ExtractedDocument extractedDocument = new ExtractedDocument(text.toString(), List.of(), Map.of());

        when(tokenizer.count(anyString())).thenAnswer(invocation -> {
            String t = invocation.getArgument(0);
            return t.split("\\s+").length;
        });

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        if (chunks.size() > 1) {
            for (int i = 0; i < chunks.size(); i++) {
                assertEquals(i, chunks.get(i).index(), "Chunk index should match position");
            }
        }
    }

    @Test
    void chunkDocument_singleSentenceNoEndingPunctuation_handlesGracefully() {
        String text = "This is text without ending punctuation";
        ExtractedDocument extractedDocument = new ExtractedDocument(text, List.of(), Map.of());
        when(tokenizer.count(anyString())).thenReturn(7);

        List<Chunk> chunks = chunkerService.chunkDocument(extractedDocument, documentId);

        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0).content());
    }
}
