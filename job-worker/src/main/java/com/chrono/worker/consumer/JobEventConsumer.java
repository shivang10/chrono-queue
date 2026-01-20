package com.chrono.worker.consumer;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.services.JobProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class JobEventConsumer {

    private static final Logger logger = Logger.getLogger(JobEventConsumer.class.getName());
    private final ObjectMapper objectMapper;
    private final JobProcessingService jobProcessingService;

    public JobEventConsumer(ObjectMapper objectMapper, JobProcessingService jobProcessingService) {
        this.objectMapper = objectMapper;
        this.jobProcessingService = jobProcessingService;
    }

    @KafkaListener(topics = {
            KafkaTopics.WEBHOOK_JOBS,
            KafkaTopics.EMAIL_JOBS,
            KafkaTopics.PAYMENT_JOBS,
            KafkaTopics.ORDER_CANCELLATION_JOBS
    }, groupId = "job-worker-group")
    public void consume(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key, Acknowledgment acknowledgment) throws Exception {
        JobEventModel jobEvent = objectMapper.readValue(message, JobEventModel.class);
        try {
            validateJob(jobEvent);
            logger.info(String.format("Consuming message - Topic: %s, Partition: %d, Offset: %d, Key: %s", topic, partition, offset, key));
            jobProcessingService.processJobEvent(jobEvent);
            logger.info(String.format("Successfully processed job: %s from topic: %s", jobEvent.getJobId(), topic));
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.severe("Error processing job event: " + e.getMessage());
            logger.warning(String.format("Job %s is set to fail (shouldJobFail=%d is divisible by 5)",
                    jobEvent.getJobId(), jobEvent.getShouldJobFail()));
            acknowledgment.acknowledge();
        }
    }


    private void validateJob(JobEventModel jobEvent) {
        if (jobEvent.getShouldJobFail() % 5 == 0) {
            throw new RuntimeException(
                    "Simulated failure: randomNumber divisible by 5 → "
                            + jobEvent.getShouldJobFail()
            );
        }
    }
}
