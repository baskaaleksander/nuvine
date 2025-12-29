package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.DocumentDownloadUrlResponse;
import com.baskaaleksander.nuvine.application.dto.UploadUrlRequest;
import com.baskaaleksander.nuvine.application.dto.UploadUrlResponse;
import com.baskaaleksander.nuvine.domain.service.DownloadService;
import com.baskaaleksander.nuvine.domain.service.UploadService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileStorageController {

    private final UploadService uploadService;
    private final DownloadService downloadService;

    @PostMapping("/upload-url")
    @RateLimiting(
            name = "upload_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<UploadUrlResponse> generatePresignedUploadUrl(
            @RequestBody @Valid UploadUrlRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(uploadService.generatePresignedUploadUrl(
                request.documentId(),
                request.contentType(),
                request.sizeBytes()
        ));
    }

    @GetMapping("/{documentId}/download-url")
    @RateLimiting(
            name = "download_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<DocumentDownloadUrlResponse> getDownloadUrl(
            @PathVariable String documentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(downloadService.getDownloadUrl(documentId));
    }

}
