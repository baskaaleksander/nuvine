package com.baskaaleksander.nuvine.application.util;

import com.baskaaleksander.nuvine.application.dto.ParsedStorageKey;

import java.util.UUID;

public final class StorageKeyUtil {

    private StorageKeyUtil() {
    }

    public static String generate(UUID workspaceId, UUID projectId, UUID documentId) {
        return "ws/%s/projects/%s/documents/%s".formatted(
                workspaceId,
                projectId,
                documentId
        );
    }

    public static ParsedStorageKey parse(String key) {

        String[] parts = key.split("/");

        if (parts.length != 6 ||
                !parts[0].equals("ws") ||
                !parts[2].equals("projects") ||
                !parts[4].equals("documents")) {
            throw new IllegalArgumentException("Invalid storageKey format: " + key);
        }

        UUID workspaceId = UUID.fromString(parts[1]);
        UUID projectId = UUID.fromString(parts[3]);
        UUID documentId = UUID.fromString(parts[5]);

        return new ParsedStorageKey(workspaceId, projectId, documentId);
    }
}