package com.chrono.dlq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import com.chrono.dlq.service.DlqHandlerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@Slf4j
@Component
public class DlqConsumer {

    private final DlqHandlerService dlqHandlerService;
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
            log.info("Consuming failed job DLQ - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    topic, partition, offset, key);
            dlqHandlerService.handleDlqMessage(jobEvent, topic);
            log.info("Successfully consumed the failed job: {} from topic: {}",
                    jobEvent.getJobId(), topic);
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobEvent.getJobId(), e.getMessage());
            log.info("Job {} cannot be saved for retry (attempt {}/{})",
                    jobEvent.getJobId(), jobEvent.getRetryCount(), jobEvent.getMaxRetries());
        } finally {
            acknowledgment.acknowledge();
        }

    }
}
