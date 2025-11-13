package com.baskaaleksander.nuvine.infrastructure.repository;

import com.baskaaleksander.nuvine.domain.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {
}
