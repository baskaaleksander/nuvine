package com.baskaaleksander.nuvine.application.dto;

import java.net.URL;

public record UploadUrlResponse(
        URL url,
        String method
) {
}
