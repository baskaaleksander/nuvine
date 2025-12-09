package com.baskaaleksander.nuvine.infrastructure.client;

import com.baskaaleksander.nuvine.infrastructure.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "subscription-service",
        url = "${application.config.api-base-url}/internal/billing/",
        contextId = "subscriptionServiceClient",
        configuration = InternalFeignConfig.class
)
public interface SubscriptionServiceClient {

    @PostMapping("/check-limit")
    CheckLimitResult checkLimit(@RequestBody CheckLimitRequest request);

    @PostMapping("/release-reservation")
    void releaseReservation(@RequestBody ReleaseReservationRequest request);
}
