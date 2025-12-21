package com.baskaaleksander.nuvine.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenCountingServiceTest {

    private TokenCountingService tokenCountingService;

    @BeforeEach
    void setUp() {
        tokenCountingService = new TokenCountingService();
    }
    
    @Test
    void count_validText_returnsPositiveCount() {
        String input = "Hello, this is a test sentence for token counting.";

        int result = tokenCountingService.count(input);

        assertTrue(result > 0, "Token count should be positive for non-empty text");
    }

    @Test
    void count_emptyText_returnsZero() {
        String input = "";

        int result = tokenCountingService.count(input);

        assertEquals(0, result);
    }

    @Test
    void count_longText_handlesCorrectly() {
        String input = "This is a longer piece of text that contains multiple sentences. "
                + "It should be tokenized correctly by the JTokkit library. "
                + "The token count should be proportional to the text length. "
                + "We are testing that the service can handle larger inputs without issues.";

        int result = tokenCountingService.count(input);

        assertTrue(result > 20, "Long text should have many tokens");
        assertTrue(result < 200, "Token count should be reasonable for this text length");
    }

    @Test
    void count_specialCharacters_handlesCorrectly() {
        String input = "Hello! @user #hashtag $100 %percent & more...";

        int result = tokenCountingService.count(input);

        assertTrue(result > 0, "Text with special characters should have positive token count");
    }
}
