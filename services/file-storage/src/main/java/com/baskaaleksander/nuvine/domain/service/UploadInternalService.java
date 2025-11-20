package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.util.StorageKeyUtil;
import com.baskaaleksander.nuvine.domain.exception.UnauthorizedWebhookException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class UploadInternalService {

    @Value("${s3.webhook.secret}")
    private String webhookSecret;


    public void handleMinioEvent(JsonNode body, String authHeader) {

        if (authHeader == null || !authHeader.equals("Bearer " + webhookSecret)) {
            throw new UnauthorizedWebhookException("Unauthorized webhook");
        }

        JsonNode record = body.path("Records").get(0);

        String bucket = record.path("s3").path("bucket").path("name").asText();
        String key = record.path("s3").path("object").path("key").asText();
        long size = record.path("s3").path("object").path("size").asLong();
        String contentType = record.path("s3").path("object").path("contentType").asText();

        log.info("MINIO_EVENT RECEIVED bucket={}, key={}, size={}, contentType={}", bucket, key, size, contentType);

        var docInfo = StorageKeyUtil.parse(key);

    }
}
