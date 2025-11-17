package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.DocumentInternalResponse;
import com.baskaaleksander.nuvine.application.dto.UpdateDocumentStatusRequest;
import com.baskaaleksander.nuvine.application.dto.UploadCompletedRequest;
import com.baskaaleksander.nuvine.domain.service.DocumentInternalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/documents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INTERNAL_SERVICE')")
public class DocumentInternalController {

    private final DocumentInternalService documentInternalService;

    @GetMapping("/{documentId}")
    public DocumentInternalResponse getDocumentById(
            @PathVariable UUID documentId
    ) {
        return documentInternalService.getDocumentById(documentId);
    }

    @PatchMapping("/{documentId}/upload-completed")
    public DocumentInternalResponse uploadCompleted(
            @RequestBody @Valid UploadCompletedRequest request,
            @PathVariable UUID documentId
    ) {
        return documentInternalService.uploadCompleted(documentId, request.storageKey(), request.mimeType(), request.sizeBytes());
    }

    @PatchMapping("/{documentId}/status")
    public DocumentInternalResponse updateStatus(
            @RequestBody @Valid UpdateDocumentStatusRequest request,
            @PathVariable UUID documentId
    ) {
        return documentInternalService.updateStatus(documentId, request.status());
    }

}
