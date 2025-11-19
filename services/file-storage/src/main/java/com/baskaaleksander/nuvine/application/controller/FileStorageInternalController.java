package com.baskaaleksander.nuvine.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/file-storage")
@RequiredArgsConstructor
public class FileStorageInternalController {


    @PostMapping("/events")
    public void handleMinioEvent(@RequestBody(required = false) String body) {
        System.out.println("MinIO event: " + body);
    }
}
