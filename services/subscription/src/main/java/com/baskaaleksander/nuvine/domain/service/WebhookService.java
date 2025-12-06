package com.baskaaleksander.nuvine.domain.service;

import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    public void handleEvent(Event event) {
        log.info("Received event: {}", event);
    }

}
