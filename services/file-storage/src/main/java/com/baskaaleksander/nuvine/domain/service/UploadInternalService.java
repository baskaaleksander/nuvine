package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.ParsedStorageKey;
import com.baskaaleksander.nuvine.application.dto.UploadCompletedRequest;
import com.baskaaleksander.nuvine.application.util.StorageKeyUtil;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Service
@Slf4j
public class UploadInternalService {

    @Value("${s3.webhook.secret}")
    private String webhookSecret;

    private final WorkspaceServiceServiceClient client;


    public void handleMinioEvent(JsonNode body, String authHeader) {

        if (authHeader == null || !authHeader.equals("Bearer " + webhookSecret)) {
            log.info("MINIO_EVENT FAILED reason=invalid_webhook_secret");
            return;
        }

        JsonNode record = body.path("Records").get(0);

        String bucket = record.path("s3").path("bucket").path("name").asText();
        String key = record.path("s3").path("object").path("key").asText();
        long size = record.path("s3").path("object").path("size").asLong();
        String contentType = record.path("s3").path("object").path("contentType").asText();

        log.info("MINIO_EVENT RECEIVED bucket={}, key={}, size={}, contentType={}", bucket, key, size, contentType);

        String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);

        ParsedStorageKey docInfo;
        try {
            docInfo = StorageKeyUtil.parse(decodedKey);
        } catch (Exception e) {
            log.error("MINIO_EVENT FAILED reason=invalid_key_format key={}", key, e);
            return;
        }

        try {
            log.info("[MINIO-HANDLER] Calling workspace uploadCompleted docId={} ...", docInfo.documentId());

            client.uploadCompleted(
                    docInfo.documentId(),
                    new UploadCompletedRequest(
                            decodedKey,
                            contentType,
                            size
                    )
            );
            log.info("[MINIO-HANDLER] Workspace uploadCompleted OK docId={}", docInfo.documentId());

        } catch (FeignException e) {
            log.error("[MINIO-HANDLER] Workspace uploadCompleted FAILED docId={}", docInfo.documentId(), e);
        }

        log.info("MINIO_EVENT HANDLED documentId={}", docInfo.documentId());

    }
}
