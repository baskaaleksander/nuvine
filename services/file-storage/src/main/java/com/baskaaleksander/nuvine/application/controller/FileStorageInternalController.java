package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.domain.service.UploadInternalService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/file-storage")
@RequiredArgsConstructor
public class FileStorageInternalController {

    private final UploadInternalService service;

    @PostMapping("/events")
    public void handleMinioEvent(
            @RequestBody JsonNode body,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        System.out.println(authHeader);
        service.handleMinioEvent(body, authHeader);
    }
}
