package com.chrono.producer.config;

import com.chrono.common.constants.KafkaTopics;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ConditionalOnProperty(prefix = "chrono.kafka.topic-management", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    private final KafkaTopicProperties kafkaTopicProperties;
    private final KafkaAdmin kafkaAdmin;

    public KafkaTopicConfig(KafkaTopicProperties kafkaTopicProperties, KafkaAdmin kafkaAdmin) {
        this.kafkaTopicProperties = kafkaTopicProperties;
        this.kafkaAdmin = kafkaAdmin;
    }

    @PostConstruct
    public void createTopics() {
        NewTopic[] topics = KafkaTopics.getAllTopics().stream()
                .map(this::buildTopic)
                .toArray(NewTopic[]::new);
        kafkaAdmin.createOrModifyTopics(topics);
    }

    private NewTopic buildTopic(String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(kafkaTopicProperties.resolvePartitions(topicName))
                .replicas(kafkaTopicProperties.getReplicationFactor())
                .build();
    }
}
