package com.baskaaleksander.nuvine.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection= "failed_notifications")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FailedNotification {
    @Id
    private String id;
    private String userId;
    private NotificationType type;
    private String encryptedPayload;
    private String payloadHash;
    private String message;

    private String originalTopic;
    private String originalPartition;
    private Long originalOffset;

    private String exceptionMessage;
    private String exceptionClass;

    private Instant failedAt;
    private Boolean replayed;
    private Instant replayedAt;
}
