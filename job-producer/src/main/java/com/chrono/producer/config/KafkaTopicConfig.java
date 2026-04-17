package com.chrono.producer.config;

import com.chrono.common.constants.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Set;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
@ConditionalOnProperty(prefix = "chrono.kafka.topic-management", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    private final KafkaTopicProperties kafkaTopicProperties;

    public KafkaTopicConfig(KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

    @Bean
    public NewTopic[] kafkaTopics() {
        Set<String> topicNames = KafkaTopics.getAllTopics();

        return topicNames.stream()
                .map(this::buildTopic)
                .toArray(NewTopic[]::new);
    }

    private NewTopic buildTopic(String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(kafkaTopicProperties.resolvePartitions(topicName))
                .replicas(kafkaTopicProperties.getReplicationFactor())
                .build();
    }
}
