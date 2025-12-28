package com.baskaaleksander.nuvine.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component("rateLimitHelper")
public class RateLimitHelper {

    public String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}