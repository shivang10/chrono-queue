package com.chrono.producer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "chrono.kafka.topic-management")
public class KafkaTopicProperties {

    private boolean enabled = true;
    private int defaultPartitions = 3;
    private short replicationFactor = 1;
    private Map<String, Integer> partitions = new HashMap<>();

    public int resolvePartitions(String topicName) {
        Integer configuredPartitions = partitions.get(topicName);
        int resolvedPartitions = configuredPartitions != null ? configuredPartitions : defaultPartitions;

        if (resolvedPartitions < 1) {
            throw new IllegalStateException("Kafka topic partitions must be greater than zero for topic: " + topicName);
        }

        return resolvedPartitions;
    }

    public short resolveReplicationFactor() {
        if (replicationFactor < 1) {
            throw new IllegalStateException("Kafka topic replication factor must be greater than zero");
        }

        return replicationFactor;
    }
}
