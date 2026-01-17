package com.baskaaleksander.nuvine.integration.kafka;

import com.baskaaleksander.nuvine.infrastructure.messaging.EmailVerificationEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.PasswordResetEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.UserRegisteredEventProducer;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.EmailVerificationEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.PasswordResetEvent;
import com.baskaaleksander.nuvine.infrastructure.messaging.dto.UserRegisteredEvent;
import com.baskaaleksander.nuvine.integration.base.BaseKafkaIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaProducersIT extends BaseKafkaIntegrationTest {

    @Autowired
    private UserRegisteredEventProducer userRegisteredEventProducer;

    @Autowired
    private EmailVerificationEventProducer emailVerificationEventProducer;

    @Autowired
    private PasswordResetEventProducer passwordResetEventProducer;

    @Test
    void userRegisteredEvent_isPublished() throws Exception {
        BlockingQueue<ConsumerRecord<String, UserRegisteredEvent>> queue =
                createConsumer("user-registered-topic-test", UserRegisteredEvent.class);

        UserRegisteredEvent event = new UserRegisteredEvent(
                "Jane",
                "Doe",
                "jane@example.com",
                "verify-token",
                UUID.randomUUID().toString()
        );

        userRegisteredEventProducer.sendUserRegisteredEvent(event);

        UserRegisteredEvent received = awaitMessage(queue, 20, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.email(), received.email());
        assertEquals(event.userId(), received.userId());
        assertEquals(event.emailVerificationToken(), received.emailVerificationToken());
    }

    @Test
    void emailVerificationEvent_isPublished() throws Exception {
        BlockingQueue<ConsumerRecord<String, EmailVerificationEvent>> queue =
                createConsumer("email-verification-topic-test", EmailVerificationEvent.class);

        EmailVerificationEvent event = new EmailVerificationEvent(
                "verify@example.com",
                "verify-token",
                UUID.randomUUID().toString()
        );

        emailVerificationEventProducer.sendEmailVerificationEvent(event);

        EmailVerificationEvent received = awaitMessage(queue, 20, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.email(), received.email());
        assertEquals(event.token(), received.token());
    }

    @Test
    void passwordResetEvent_isPublished() throws Exception {
        BlockingQueue<ConsumerRecord<String, PasswordResetEvent>> queue =
                createConsumer("password-reset-topic-test", PasswordResetEvent.class);

        PasswordResetEvent event = new PasswordResetEvent(
                "reset@example.com",
                "reset-token",
                UUID.randomUUID().toString()
        );

        passwordResetEventProducer.sendPasswordResetEvent(event);

        PasswordResetEvent received = awaitMessage(queue, 20, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(event.email(), received.email());
        assertEquals(event.token(), received.token());
    }
}
