package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentDownloadUrlResponse;
import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.domain.exception.DocumentAccessForbiddenException;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotUploadedException;
import com.baskaaleksander.nuvine.domain.model.DocumentStatus;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceUserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadService {

    private final WorkspaceServiceUserClient client;
    private final S3Presigner s3Presigner;

    @Value("${s3.bucket-name}")
    private String bucketName;

    public DocumentDownloadUrlResponse getDownloadUrl(String documentId) {

        log.info("GENERATE_DOWNLOAD_URL START documentId={}", documentId);

        DocumentInternalResponse documentInternalResponse;

        try {
            documentInternalResponse = client.getInternalDocument(documentId);
        } catch (FeignException ex) {
            int status = ex.status();

            switch (status) {
                case 404:
                    log.info("GENERATE_DOWNLOAD_URL FAILED reason=document_not_found documentId={}", documentId);
                    throw new DocumentNotFoundException("Document not found");
                case 403:
                    log.info("GENERATE_DOWNLOAD_URL FAILED reason=access_forbidden documentId={}", documentId);
                    throw new DocumentAccessForbiddenException("Access forbidden");
                default:
                    log.error("GENERATE_DOWNLOAD_URL FAILED documentId={}", documentId, ex);
                    throw new RuntimeException(ex);
            }
        }

        if (documentInternalResponse.status() == DocumentStatus.UPLOADING || documentInternalResponse.status() == DocumentStatus.FAILED) {
            log.info("GENERATE_DOWNLOAD_URL FAILED reason=document_not_uploaded documentId={}", documentId);
            throw new DocumentNotUploadedException("Document is not uploaded yet");
        }

        URL url;
        try {
            url = presignDownloadUrl(documentInternalResponse.storageKey(), documentInternalResponse.mimeType(), documentInternalResponse.name());
        } catch (Exception ex) {
            log.error("GENERATE_DOWNLOAD_URL FAILED documentId={}", documentId, ex);
            throw new RuntimeException("Failed to generate download URL");
        }
        log.info("GENERATE_DOWNLOAD_URL END documentId={}", documentId);

        return new DocumentDownloadUrlResponse(
                url.toString()
        );

    }

    private URL presignDownloadUrl(String key, String mimeType, String name) {

        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .responseContentType(mimeType)
                .responseContentDisposition("attachment; filename=\"" + name + "\"")
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(getReq)
                        .build()
        );

        return presigned.url();
    }
}
