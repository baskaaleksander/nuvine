package com.baskaaleksander.nuvine.integration.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

public abstract class BaseKafkaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;
}
