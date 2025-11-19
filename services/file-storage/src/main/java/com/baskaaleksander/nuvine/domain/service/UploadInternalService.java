package com.baskaaleksander.nuvine.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class UploadInternalService {


    public void handleMinioEvent(JsonNode body) {
        JsonNode record = body.path("Records").get(0);
        
        String bucket = record.path("s3").path("bucket").path("name").asText();
        String key = record.path("s3").path("object").path("key").asText();
        long size = record.path("s3").path("object").path("size").asLong();
        String contentType = record.path("s3").path("object").path("contentType").asText();

        log.info("MINIO_EVENT bucket={}, key={}, size={}, contentType={}", bucket, key, size, contentType);
    }
}
