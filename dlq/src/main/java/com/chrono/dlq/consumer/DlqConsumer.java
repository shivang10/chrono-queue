package com.chrono.dlq.consumer;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import com.chrono.dlq.service.DlqHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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
            Acknowledgment acknowledgment) {
        JobEventModel jobEvent;

        try {
            jobEvent = objectMapper.readValue(message, JobEventModel.class);
        } catch (Exception ex) {
            log.error("Failed to deserialize DLQ message from topic:{} partition:{} offset:{} key:{}",
                    topic, partition, offset, key, ex);
            throw new IllegalStateException("DLQ message deserialization failed", ex);
        }

        try {
            log.info("Consuming failed job DLQ - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    topic, partition, offset, key);
            dlqHandlerService.handleDlqMessage(jobEvent, topic);
            dlqHandlerService.saveFailedJob(jobEvent);
            log.info("Successfully consumed the failed job: {} from topic: {}",
                    jobEvent.getJobId(), topic);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to persist DLQ job {} from topic {}",
                    jobEvent.getJobId(), topic, ex);
            throw ex;
        }

    }
}
