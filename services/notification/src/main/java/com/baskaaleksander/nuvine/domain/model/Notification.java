package com.baskaaleksander.nuvine.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document
@AllArgsConstructor @NoArgsConstructor
@Getter @Setter
@Builder
public class Notification {

    @Id
    private String id;
    private String userId;
    private NotificationType type;
    private String message;
    private Instant createdAt;
}
