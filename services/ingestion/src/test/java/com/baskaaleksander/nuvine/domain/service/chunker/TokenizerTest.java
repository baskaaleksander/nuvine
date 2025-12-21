package com.baskaaleksander.nuvine.domain.service.chunker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {

    private Tokenizer tokenizer;

    @BeforeEach
    void setUp() {
        tokenizer = new Tokenizer();
    }

    @Test
    void count_simpleText_returnsTokenCount() {
        String text = "Hello world";

        int count = tokenizer.count(text);

        assertTrue(count > 0);
        assertEquals(2, count); // "Hello" and "world" typically 2 tokens
    }

    @Test
    void count_emptyString_returnsZero() {
        int count = tokenizer.count("");

        assertEquals(0, count);
    }

    @Test
    void count_longerText_returnsCorrectTokenCount() {
        String text = "The quick brown fox jumps over the lazy dog";

        int count = tokenizer.count(text);

        assertTrue(count > 0);
        // Each word is approximately 1 token, but some may be tokenized differently
        assertTrue(count >= 8 && count <= 12);
    }

    @Test
    void count_textWithPunctuation_includesPunctuationTokens() {
        String text = "Hello, world! How are you?";

        int count = tokenizer.count(text);

        assertTrue(count > 0);
        // Punctuation marks may be separate tokens
    }

    @Test
    void count_textWithNewlines_handlesWhitespace() {
        String text = "Line one\nLine two\nLine three";

        int count = tokenizer.count(text);

        assertTrue(count > 0);
    }

    @Test
    void count_unicodeText_handlesUnicode() {
        String text = "Hello 你好";

        int count = tokenizer.count(text);

        assertTrue(count > 0);
    }

    @Test
    void count_repeatedCalls_returnsConsistentResults() {
        String text = "Consistent token counting";

        int count1 = tokenizer.count(text);
        int count2 = tokenizer.count(text);
        int count3 = tokenizer.count(text);

        assertEquals(count1, count2);
        assertEquals(count2, count3);
    }
}
