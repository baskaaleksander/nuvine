package com.baskaaleksander.nuvine.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentFetcherTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private DocumentFetcher documentFetcher;

    private String bucketName = "test-bucket";
    private String storageKey = "workspaces/123/documents/456.pdf";
    private byte[] expectedBytes = "test content".getBytes();

    @BeforeEach
    void setUp() throws Exception {
        // Set the bucket field using reflection since it's @Value injected
        Field bucketField = DocumentFetcher.class.getDeclaredField("bucket");
        bucketField.setAccessible(true);
        bucketField.set(documentFetcher, bucketName);
    }

    @Test
    void fetch_validKey_returnsBytes() throws IOException {
        ResponseInputStream<GetObjectResponse> responseInputStream = 
                new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        new ByteArrayInputStream(expectedBytes)
                );
        
        when(s3Client.getObject(any(Consumer.class))).thenReturn(responseInputStream);

        byte[] result = documentFetcher.fetch(storageKey);

        assertArrayEquals(expectedBytes, result);
        verify(s3Client).getObject(any(Consumer.class));
    }

    @Test
    void fetch_notFound_throwsRuntimeException() {
        when(s3Client.getObject(any(Consumer.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentFetcher.fetch(storageKey)
        );
        assertTrue(exception.getMessage().contains("Failed to fetch document from S3"));
    }

    @Test
    void fetch_s3Error_throwsRuntimeException() {
        when(s3Client.getObject(any(Consumer.class)))
                .thenThrow(S3Exception.builder().message("S3 error").build());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentFetcher.fetch(storageKey)
        );
        assertTrue(exception.getMessage().contains("Failed to fetch document from S3"));
    }

    @Test
    void fetch_ioException_throwsRuntimeException() {
        when(s3Client.getObject(any(Consumer.class)))
                .thenThrow(new RuntimeException("IO error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentFetcher.fetch(storageKey)
        );
        assertNotNull(exception);
    }
}
