package com.baskaaleksander.nuvine.domain.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownCleanerTest {

    // Tests for clean() method

    @Test
    void clean_nullInput_returnsEmptyString() {
        String result = MarkdownCleaner.clean(null);

        assertEquals("", result);
    }

    @Test
    void clean_headings_stripsHashSymbols() {
        String input = "# Heading 1\n## Heading 2\n### Heading 3";

        String result = MarkdownCleaner.clean(input);

        assertEquals("Heading 1 Heading 2 Heading 3", result);
    }

    @Test
    void clean_bold_stripsBoldMarkers() {
        String input = "This is **bold** text";

        String result = MarkdownCleaner.clean(input);

        assertEquals("This is bold text", result);
    }

    @Test
    void clean_italic_stripsItalicMarkers() {
        String input = "This is *italic* text";

        String result = MarkdownCleaner.clean(input);

        assertEquals("This is italic text", result);
    }

    @Test
    void clean_inlineCode_removesBackticks() {
        String input = "Use `code` here";

        String result = MarkdownCleaner.clean(input);

        assertEquals("Use code here", result);
    }

    @Test
    void clean_strikethrough_removesMarkers() {
        String input = "This is ~~strikethrough~~ text";

        String result = MarkdownCleaner.clean(input);

        assertEquals("This is strikethrough text", result);
    }

    @Test
    void clean_complexMarkdown_cleansAll() {
        String input = "# Title\n**Bold** and *italic* with `code`\n> quote\n- list item";

        String result = MarkdownCleaner.clean(input);

        assertEquals("Title Bold and italic with code quote list item", result);
    }
}
