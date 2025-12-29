package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.*;
import com.baskaaleksander.nuvine.domain.service.DocumentService;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PreAuthorize("@projectAccess.canManageProject(#projectId, #jwt.getSubject())")
    @PostMapping("/api/v1/projects/{projectId}/documents")
    @RateLimiting(
            name = "create_document_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<DocumentPublicResponse> createDocument(
            @RequestBody @Valid CreateDocumentRequest request,
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(documentService.createDocument(request.name(), UUID.fromString(jwt.getSubject()), projectId));
    }

    //todo add filters
    @PreAuthorize("@projectAccess.canGetProject(#projectId, #jwt.getSubject())")
    @GetMapping("/api/v1/projects/{projectId}/documents")
    public ResponseEntity<PagedResponse<DocumentPublicResponse>> getDocuments(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return ResponseEntity.ok(documentService.getDocuments(projectId, request));
    }

    @PreAuthorize("@docAccess.canGetDocument(#documentId, #jwt.getSubject())")
    @GetMapping("/api/v1/documents/{documentId}")
    public ResponseEntity<DocumentPublicResponse> getDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(documentService.getDocument(documentId));
    }

    @PreAuthorize("@docAccess.canManageDocument(#documentId, #jwt.getSubject())")
    @PatchMapping("/api/v1/documents/{documentId}")
    @RateLimiting(
            name = "update_document_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<DocumentPublicResponse> updateDocument(
            @PathVariable UUID documentId,
            @RequestBody @Valid UpdateDocumentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(documentService.updateDocument(documentId, request.name()));
    }

    @PreAuthorize("@docAccess.canManageDocument(#documentId, #jwt.getSubject())")
    @DeleteMapping("/api/v1/documents/{documentId}")
    @RateLimiting(
            name = "delete_document_limit",
            cacheKey = "@jwt.getSubject()",
            ratePerMethod = true
    )
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
