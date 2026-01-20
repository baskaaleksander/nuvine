package com.baskaaleksander.nuvine.integration.support;

import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class S3TestUtils {

    private final S3Client s3Client;
    private final String bucketName;

    public S3TestUtils(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    public void uploadTestDocument(String storageKey, String content) {
        ensureBucketExists();
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType("text/plain")
                .build(),
            RequestBody.fromString(content)
        );
    }

    public void uploadTestDocument(String storageKey, byte[] content, String contentType) {
        ensureBucketExists();
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(content)
        );
    }

    public void uploadTestDocumentFromResource(String storageKey, String resourcePath, String contentType) {
        ensureBucketExists();
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] content = inputStream.readAllBytes();
                s3Client.putObject(
                    PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storageKey)
                        .contentType(contentType)
                        .build(),
                    RequestBody.fromBytes(content)
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload test document from resource: " + resourcePath, e);
        }
    }

    public void cleanBucket() {
        try {
            ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(bucketName).build()
            );

            List<ObjectIdentifier> objectsToDelete = response.contents().stream()
                .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
                .toList();

            if (!objectsToDelete.isEmpty()) {
                s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectsToDelete).build())
                    .build());
            }
        } catch (NoSuchBucketException e) {
            // Bucket doesn't exist, nothing to clean
        }
    }

    public byte[] getObject(String storageKey) {
        try (InputStream inputStream = s3Client.getObject(
            GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build()
        )) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read object from S3: " + storageKey, e);
        }
    }

    public boolean objectExists(String storageKey) {
        try {
            s3Client.headObject(
                HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storageKey)
                    .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
