package com.baskaaleksander.nuvine.domain.service;

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
    @Value("${s3.bucket-name}")
    private String bucket;

    public URL generatePresignedUploadUrl(String documentId, String contentType, Long sizeBytes) {

        // todo call to internal workspace service to retrieve document data
        String objectKey = String.format(documentId);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .contentType(contentType)
                .contentLength(sizeBytes)
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(objectRequest)
                .signatureDuration(Duration.ofMinutes(15))
                .build();

        PresignedPutObjectRequest presignedRequest =
                s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url();
    }
}
