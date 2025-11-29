package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.application.dto.TextVectorSearchRequest;
import com.baskaaleksander.nuvine.application.dto.VectorSearchResponse;
import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "vector-service",
        url = "${application.config.api-base-url}",
        contextId = "vectorServiceClient",
        configuration = InternalFeignConfig.class
)
public interface VectorServiceClient {

    @PostMapping("/internal/vector/search-by-text")
    VectorSearchResponse searchText(
            @RequestBody TextVectorSearchRequest request
    );

}
