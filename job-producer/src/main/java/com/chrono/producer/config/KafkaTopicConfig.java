package com.chrono.producer.config;

import com.chrono.common.constants.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Slf4j
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ConditionalOnProperty(prefix = "chrono.kafka.topic-management", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    private final KafkaTopicProperties kafkaTopicProperties;

    public KafkaTopicConfig(KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

    @Bean
    public org.springframework.kafka.core.KafkaAdmin.NewTopics chronoKafkaTopics() {
        NewTopic[] topics = KafkaTopics.getAllTopics().stream()
                .sorted()
                .map(this::buildTopic)
                .toArray(NewTopic[]::new);
        log.info("Registering Kafka topics - count: {}, topics: {}",
                topics.length, KafkaTopics.getAllTopics());

        return new org.springframework.kafka.core.KafkaAdmin.NewTopics(topics);
    }

    private NewTopic buildTopic(String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(kafkaTopicProperties.resolvePartitions(topicName))
                .replicas(kafkaTopicProperties.resolveReplicationFactor())
                .build();
    }
}
