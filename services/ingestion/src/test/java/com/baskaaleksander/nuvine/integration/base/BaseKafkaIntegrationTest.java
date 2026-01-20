package com.baskaaleksander.nuvine.integration.base;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.ContainerTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.baskaaleksander.nuvine.integration.config.TestContainersConfig.KAFKA;

public abstract class BaseKafkaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    protected <T> BlockingQueue<ConsumerRecord<String, T>> createConsumer(String topic, Class<T> valueType) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + topic + "-" + java.util.UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        ensureTopicExists(topic);

        DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);

        BlockingQueue<ConsumerRecord<String, T>> records = new LinkedBlockingQueue<>();

        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener((MessageListener<String, T>) records::add);

        KafkaMessageListenerContainer<String, T> container =
            new KafkaMessageListenerContainer<>(consumerFactory, containerProps);

        container.start();
        ContainerTestUtils.waitForAssignment(container, 1);

        kafkaTemplate.flush();

        return records;
    }

    protected <T> T awaitMessage(BlockingQueue<ConsumerRecord<String, T>> queue, int timeout, TimeUnit unit)
            throws InterruptedException {
        ConsumerRecord<String, T> record = queue.poll(timeout, unit);
        return record != null ? record.value() : null;
    }

    protected void ensureTopicExists(String topic) {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()
        );
        try (AdminClient admin = AdminClient.create(config)) {
            NewTopic newTopic = new NewTopic(topic, 1, (short) 1);
            admin.createTopics(List.of(newTopic)).all().get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new IllegalStateException("Failed to create topic " + topic, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating topic " + topic, e);
        }

        try (AdminClient admin = AdminClient.create(config)) {
            admin.describeTopics(List.of(topic)).allTopicNames().get();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to verify topic " + topic, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while verifying topic " + topic, e);
        }
    }
}
