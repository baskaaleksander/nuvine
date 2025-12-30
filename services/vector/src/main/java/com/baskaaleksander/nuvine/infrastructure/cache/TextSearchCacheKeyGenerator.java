package com.baskaaleksander.nuvine.infrastructure.cache;

import com.baskaaleksander.nuvine.application.dto.TextVectorSearchRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Component
public class TextSearchCacheKeyGenerator {
    
    public String generateKey(TextVectorSearchRequest request) {
        List<String> sortedDocIds = request.documentIds().stream()
                .map(UUID::toString)
                .sorted()
                .toList();

        String docIdsHash = hashString(String.join(",", sortedDocIds));
        String queryHash = hashString(request.query());

        return String.format("%s:%s:%s:%s:%d:%.4f",
                request.workspaceId(),
                request.projectId(),
                docIdsHash,
                queryHash,
                request.topK(),
                request.threshold()
        );
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
