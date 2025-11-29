package com.baskaaleksander.nuvine.domain.service;

public final class MarkdownCleaner {

    private MarkdownCleaner() {
    }

    public static String clean(String input) {
        if (input == null) return "";

        String cleaned = input;

        cleaned = cleaned.replaceAll("(?m)^#{1,6}\\s*", "");
        cleaned = cleaned.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        cleaned = cleaned.replaceAll("\\*(.*?)\\*", "$1");
        cleaned = cleaned.replaceAll("~~(.*?)~~", "$1");
        cleaned = cleaned.replaceAll("`([^`]*)`", "$1");
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "");
        cleaned = cleaned.replaceAll(">\\s*", "");
        cleaned = cleaned.replaceAll("[-*+]\\s+", "");
        cleaned = cleaned.replaceAll("\\r?\\n", " ");

        return cleaned.trim();
    }
}