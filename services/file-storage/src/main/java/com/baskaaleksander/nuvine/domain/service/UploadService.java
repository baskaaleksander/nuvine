package com.baskaaleksander.nuvine.domain.service;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.infrastructure.client.WorkspaceServiceUserClient;
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

        DocumentInternalResponse documentInternalResponse = workspaceServiceUserClient.getInternalDocument(documentId);

        String key = String.format("ws/%s/proj/%s/docs/%s", documentInternalResponse.workspaceId(), documentInternalResponse.projectId(), documentInternalResponse.id());

        System.out.println(key);

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
