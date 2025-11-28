package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.application.dto.TextVectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/vector")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL_SERVICE')")
public class VectorInternalController {


    @PostMapping("/search-by-text")
    public ResponseEntity<VectorSearchResponse> searchByText(
            @RequestBody @Valid TextVectorSearchRequest request
    ) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/search")
    public ResponseEntity<VectorSearchResponse> search(
            @RequestBody @Valid VectorSearchRequest request
    ) {
        return ResponseEntity.ok().build();
    }
}
