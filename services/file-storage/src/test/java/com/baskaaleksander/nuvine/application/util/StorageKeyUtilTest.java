package com.baskaaleksander.nuvine.application.util;

import com.baskaaleksander.nuvine.application.dto.ParsedStorageKey;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StorageKeyUtilTest {

    private final UUID workspaceId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private final UUID projectId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private final UUID documentId = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");

    @Test
    void generate_validIds_createsCorrectPath() {
        String result = StorageKeyUtil.generate(workspaceId, projectId, documentId);

        assertEquals("ws/550e8400-e29b-41d4-a716-446655440000/projects/660e8400-e29b-41d4-a716-446655440001/documents/770e8400-e29b-41d4-a716-446655440002", result);
    }

    @Test
    void generate_differentIds_createsUniquePaths() {
        UUID otherWorkspaceId = UUID.randomUUID();
        UUID otherProjectId = UUID.randomUUID();
        UUID otherDocumentId = UUID.randomUUID();

        String path1 = StorageKeyUtil.generate(workspaceId, projectId, documentId);
        String path2 = StorageKeyUtil.generate(otherWorkspaceId, otherProjectId, otherDocumentId);

        assertNotEquals(path1, path2);
    }

    @Test
    void parse_validKey_extractsWorkspaceId() {
        String key = "ws/550e8400-e29b-41d4-a716-446655440000/projects/660e8400-e29b-41d4-a716-446655440001/documents/770e8400-e29b-41d4-a716-446655440002";

        ParsedStorageKey result = StorageKeyUtil.parse(key);

        assertEquals(workspaceId, result.workspaceId());
    }

    @Test
    void parse_validKey_extractsProjectId() {
        String key = "ws/550e8400-e29b-41d4-a716-446655440000/projects/660e8400-e29b-41d4-a716-446655440001/documents/770e8400-e29b-41d4-a716-446655440002";

        ParsedStorageKey result = StorageKeyUtil.parse(key);

        assertEquals(projectId, result.projectId());
    }

    @Test
    void parse_validKey_extractsDocumentId() {
        String key = "ws/550e8400-e29b-41d4-a716-446655440000/projects/660e8400-e29b-41d4-a716-446655440001/documents/770e8400-e29b-41d4-a716-446655440002";

        ParsedStorageKey result = StorageKeyUtil.parse(key);

        assertEquals(documentId, result.documentId());
    }

    @Test
    void parse_invalidFormat_missingWsPrefix_throwsException() {
        String invalidKey = "workspace/550e8400-e29b-41d4-a716-446655440000/projects/660e8400-e29b-41d4-a716-446655440001/documents/770e8400-e29b-41d4-a716-446655440002";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StorageKeyUtil.parse(invalidKey)
        );

        assertTrue(exception.getMessage().contains("Invalid storageKey format"));
    }

    @Test
    void parse_invalidFormat_missingProjectsSegment_throwsException() {
        String invalidKey = "ws/550e8400-e29b-41d4-a716-446655440000/proj/660e8400-e29b-41d4-a716-446655440001/documents/770e8400-e29b-41d4-a716-446655440002";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StorageKeyUtil.parse(invalidKey)
        );

        assertTrue(exception.getMessage().contains("Invalid storageKey format"));
    }

    @Test
    void parse_invalidFormat_missingDocumentsSegment_throwsException() {
        String invalidKey = "ws/550e8400-e29b-41d4-a716-446655440000/projects/660e8400-e29b-41d4-a716-446655440001/docs/770e8400-e29b-41d4-a716-446655440002";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StorageKeyUtil.parse(invalidKey)
        );

        assertTrue(exception.getMessage().contains("Invalid storageKey format"));
    }

    @Test
    void parse_invalidFormat_wrongSegmentCount_throwsException() {
        String invalidKey = "ws/550e8400-e29b-41d4-a716-446655440000/projects/660e8400-e29b-41d4-a716-446655440001";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StorageKeyUtil.parse(invalidKey)
        );

        assertTrue(exception.getMessage().contains("Invalid storageKey format"));
    }

    @Test
    void parse_invalidUuid_throwsException() {
        String invalidKey = "ws/not-a-uuid/projects/660e8400-e29b-41d4-a716-446655440001/documents/770e8400-e29b-41d4-a716-446655440002";

        assertThrows(
                IllegalArgumentException.class,
                () -> StorageKeyUtil.parse(invalidKey)
        );
    }

    @Test
    void generate_then_parse_roundTrip_returnsOriginalIds() {
        String generatedKey = StorageKeyUtil.generate(workspaceId, projectId, documentId);

        ParsedStorageKey parsed = StorageKeyUtil.parse(generatedKey);

        assertEquals(workspaceId, parsed.workspaceId());
        assertEquals(projectId, parsed.projectId());
        assertEquals(documentId, parsed.documentId());
    }
}
