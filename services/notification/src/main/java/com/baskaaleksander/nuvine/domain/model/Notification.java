package com.baskaaleksander.nuvine.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection= "notifications")
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
@Builder
public class Notification {

    @Id
    private String id;
    private String userId;
    private NotificationType type;
    private String encryptedPayload;
    private String payloadHash;
    private Instant createdAt;
}
