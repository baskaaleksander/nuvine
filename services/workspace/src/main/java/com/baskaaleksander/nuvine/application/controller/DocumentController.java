package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.CreateDocumentRequest;
import com.baskaaleksander.nuvine.application.dto.DocumentResponse;
import com.baskaaleksander.nuvine.application.dto.PagedResponse;
import com.baskaaleksander.nuvine.application.dto.PaginationRequest;
import com.baskaaleksander.nuvine.domain.service.DocumentService;
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
    public ResponseEntity<DocumentResponse> createDocument(
            @RequestBody @Valid CreateDocumentRequest request,
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(documentService.createDocument(request.name(), UUID.fromString(jwt.getSubject()), projectId));
    }


    //todo add filters
    @PreAuthorize("@projectAccess.canGetProject(#projectId, #jwt.getSubject())")
    @GetMapping("/api/v1/projects/{projectId}/documents")
    public ResponseEntity<PagedResponse<DocumentResponse>> getDocuments(
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
}
