package com.baskaaleksander.nuvine.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentFetcher {

    private final S3Client s3Client;

    @Value("${s3.bucket-name}")
    private String bucket;

    public byte[] fetch(String storageKey) {
        log.info("S3_DOC_FETCH STARTED");
        try {
            return s3Client.getObject(r -> r.bucket(bucket).key(storageKey)).readAllBytes();
        } catch (Exception e) {
            log.error("S3_DOC_FETCH FAILED", e);
            throw new RuntimeException("Failed to fetch document from S3", e);
        }
    }
}
