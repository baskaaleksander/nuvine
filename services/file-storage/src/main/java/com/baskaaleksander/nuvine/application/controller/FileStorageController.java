package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.UploadUrlRequest;
import com.baskaaleksander.nuvine.domain.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileStorageController {

    private final UploadService uploadService;

    @PostMapping("/upload-url")
    public ResponseEntity<URL> generatePresignedUploadUrl(
            @RequestBody @Valid UploadUrlRequest request
    ) {
        return ResponseEntity.ok(uploadService.generatePresignedUploadUrl(request.documentId(), request.contentType(), request.sizeBytes()));
    }
}
