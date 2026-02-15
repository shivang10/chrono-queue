package com.chrono.worker.consumer;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.enums.JobStatus;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.services.JobProcessingService;
import com.chrono.worker.services.retry.RetryHandler;
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
    private final RetryHandler retryHandler;

    public JobEventConsumer(ObjectMapper objectMapper,
                            JobProcessingService jobProcessingService,
                            RetryHandler retryHandler) {
        this.objectMapper = objectMapper;
        this.jobProcessingService = jobProcessingService;
        this.retryHandler = retryHandler;
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
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        try {
            JobEventModel jobEvent = objectMapper.readValue(message, JobEventModel.class);
            try {
                validateJob(jobEvent);
                logger.info(String.format("Consuming message - Topic: %s, Partition: %d, Offset: %d, Key: %s",
                        topic, partition, offset, key));
                jobProcessingService.processJobEvent(jobEvent);
                jobEvent.setStatus(JobStatus.COMPLETED);
                logger.info(String.format("Successfully processed job: %s from topic: %s",
                        jobEvent.getJobId(), topic));
            } catch (Exception e) {
                logger.severe(String.format("Job %s failed: %s", jobEvent.getJobId(), e.getMessage()));
                jobEvent.setStatus(JobStatus.FAILED);
                retryHandler.handleFailure(jobEvent, e);
                logger.info(String.format("Job %s scheduled for retry (attempt %d/%d)",
                        jobEvent.getJobId(), jobEvent.getRetryCount(), jobEvent.getMaxRetries()));
            }
        } catch (Exception e) {
            logger.severe("Fatal error processing message: " + e.getMessage());
        } finally {
            acknowledgment.acknowledge();
        }
    }

    private void validateJob(JobEventModel jobEvent) {
        int maxRetries = jobEvent.getMaxRetries();
        int failUntilAttempt = jobEvent.getFailUntilAttempt();
        if(failUntilAttempt > maxRetries) {
            throw new RuntimeException(
                    "Simulated failure: failUntilAttempt exceeded maxRetries → "
                            + failUntilAttempt
            );
        }

        double probabilisticFailure = 0.3 / (maxRetries + 1);
        double currentRequiredVal = Math.random();
        logger.info("Current Requirer Value is: " +  currentRequiredVal + " and the value of job is: " +  probabilisticFailure);

        if(currentRequiredVal < probabilisticFailure) {
            throw new RuntimeException(
                    "Simulated failure: probabilistic failure triggered → "
                            + failUntilAttempt
            );
        }
    }
}
