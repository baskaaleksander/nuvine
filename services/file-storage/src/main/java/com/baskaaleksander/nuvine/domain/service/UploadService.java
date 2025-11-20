package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.dto.UploadUrlResponse;
import com.baskaaleksander.nuvine.application.util.StorageKeyUtil;
import com.baskaaleksander.nuvine.domain.exception.DocumentAccessForbiddenException;
import com.baskaaleksander.nuvine.domain.exception.DocumentConflictException;
import com.baskaaleksander.nuvine.domain.exception.DocumentNotFoundException;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceUserClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class UploadService {
    private final S3Presigner s3Presigner;
    private final WorkspaceServiceUserClient workspaceServiceUserClient;

    @Value("${s3.bucket-name}")
    private String bucket;

    private final String UPLOADING_STATUS = "UPLOADING";

    public UploadUrlResponse generatePresignedUploadUrl(String documentId, String contentType, Long sizeBytes) {

        log.info("GENERATE_UPLOAD_URL STARTED documentId={} contentType={} sizeBytes={}", documentId, contentType, sizeBytes);

        DocumentInternalResponse documentInternalResponse;

        try {
            documentInternalResponse = workspaceServiceUserClient.getInternalDocument(documentId);
        } catch (FeignException ex) {
            int status = ex.status();

            switch (status) {
                case 404:
                    log.info("GENERATE_UPLOAD_URL FAILED reason=document_not_found documentId={} contentType={} sizeBytes={}", documentId, contentType, sizeBytes);
                    throw new DocumentNotFoundException("Document not found");
                case 403:
                    log.info("GENERATE_UPLOAD_URL FAILED reason=access_forbidden documentId={} contentType={} sizeBytes={}", documentId, contentType, sizeBytes);
                    throw new DocumentAccessForbiddenException("Access forbidden");
                default:
                    log.error("GENERATE_UPLOAD_URL FAILED documentId={} contentType={} sizeBytes={}", documentId, contentType, sizeBytes, ex);
                    throw new RuntimeException(ex);
            }
        }

        if (!documentInternalResponse.status().equals(UPLOADING_STATUS)) {
            log.info("GENERATE_UPLOAD_URL FAILED reason=document_not_uploading documentId={} contentType={} sizeBytes={}", documentId, contentType, sizeBytes);
            throw new DocumentConflictException("Document already uploaded");
        }

        String key = StorageKeyUtil.generate(
                documentInternalResponse.workspaceId(),
                documentInternalResponse.projectId(),
                documentInternalResponse.id()
        );

        URL url;

        try {
            url = presignedUploadUrl(key, contentType, sizeBytes);
        } catch (Exception ex) {
            log.error("GENERATE_UPLOAD_URL FAILED documentId={} contentType={} sizeBytes={}", documentId, contentType, sizeBytes, ex);
            throw new RuntimeException("Failed to generate upload URL");
        }


        log.info("GENERATE_UPLOAD_URL END documentId={} contentType={} sizeBytes={}", documentId, contentType, sizeBytes);

        return new UploadUrlResponse(url, "PUT");
    }

    private URL presignedUploadUrl(String key, String contentType, Long sizeBytes) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .contentType(contentType)
                .contentLength(sizeBytes)
                .key(key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(15))
                .build();

        PresignedPutObjectRequest presignedRequest =
                s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url();
    }

}
