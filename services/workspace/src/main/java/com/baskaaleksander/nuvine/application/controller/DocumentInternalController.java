package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.dto.UpdateDocumentStatusRequest;
import com.baskaaleksander.nuvine.application.dto.UploadCompletedRequest;
import com.baskaaleksander.nuvine.domain.service.DocumentInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/documents")
@RequiredArgsConstructor
public class DocumentInternalController {

    private final DocumentInternalService documentInternalService;

    @PreAuthorize("@docAccess.canManageDocument(#documentId, #jwt.getSubject())")
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentInternalResponse> getDocumentById(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(documentInternalService.getDocumentById(documentId));
    }

    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @PatchMapping("/{documentId}/upload-completed")
    public ResponseEntity<DocumentInternalResponse> uploadCompleted(
            @RequestBody @Valid UploadCompletedRequest request,
            @PathVariable UUID documentId
    ) {
        return ResponseEntity.ok(documentInternalService.uploadCompleted(documentId, request.storageKey(), request.mimeType(), request.sizeBytes()));
    }

    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    @PatchMapping("/{documentId}/status")
    public ResponseEntity<DocumentInternalResponse> updateStatus(
            @RequestBody @Valid UpdateDocumentStatusRequest request,
            @PathVariable UUID documentId
    ) {
        return ResponseEntity.ok(documentInternalService.updateStatus(documentId, request.status()));
    }

}
