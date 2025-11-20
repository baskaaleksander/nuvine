package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.util.StorageKeyUtil;
import com.baskaaleksander.nuvine.domain.exception.DocumentAccessForbiddenException;
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

    public URL generatePresignedUploadUrl(String documentId, String contentType, Long sizeBytes) {

        DocumentInternalResponse documentInternalResponse;

        try {
            documentInternalResponse = workspaceServiceUserClient.getInternalDocument(documentId);
        } catch (FeignException ex) {
            int status = ex.status();

            switch (status) {
                case 404:
                    throw new DocumentNotFoundException("Document not found");
                case 403:
                    throw new DocumentAccessForbiddenException("Access forbidden");
                default:
                    throw new RuntimeException(ex);
            }
        }

        String key = StorageKeyUtil.generate(
                documentInternalResponse.workspaceId(),
                documentInternalResponse.projectId(),
                documentInternalResponse.id()
        );

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
