package com.baskaaleksander.nuvine.application.controller;

import com.baskaaleksander.nuvine.domain.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
}
