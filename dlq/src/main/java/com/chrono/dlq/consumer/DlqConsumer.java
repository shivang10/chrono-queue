package com.chrono.dlq.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import com.chrono.dlq.service.DlqHandlerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.logging.Logger;

import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@Component
public class DlqConsumer {

    private final DlqHandlerService dlqHandlerService;
    private static final Logger logger = Logger.getLogger(DlqConsumer.class.getName());
    private final ObjectMapper objectMapper;

    public DlqConsumer(ObjectMapper objectMapper, DlqHandlerService dlqHandlerService) {
        this.objectMapper = objectMapper;
        this.dlqHandlerService = dlqHandlerService;
    }

    @KafkaListener(topics = {
            KafkaTopics.JOB_DLQ
    }, groupId = "dlq-group")
    public void consume(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) throws JsonMappingException, JsonProcessingException {
        JobEventModel jobEvent = objectMapper.readValue(message, JobEventModel.class);
        try {
            logger.info(String.format("Consuming failed job DLQ - Topic: %s, Partition: %d, Offset: %d, Key: %s",
                    topic, partition, offset, key));
            dlqHandlerService.handleDlqMessage(jobEvent, topic);
            logger.info(String.format("Successfully consumed the failed job: %s from topic: %s",
                    jobEvent.getJobId(), topic));
        } catch (Exception e) {
            logger.severe(String.format("Job %s failed: %s", jobEvent.getJobId(), e.getMessage()));
            logger.info(String.format("Job %s cannot be saved for retry (attempt %d/%d)",
                    jobEvent.getJobId(), jobEvent.getRetryCount(), jobEvent.getMaxRetries()));
        } finally {
            acknowledgment.acknowledge();
        }

    }
}
