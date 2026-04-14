package com.chrono.worker.consumer;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.enums.JobStatus;
import com.chrono.common.exceptions.JobExecutionException;
import com.chrono.common.model.JobEventModel;
import com.chrono.common.validation.JobPayloadValidator;
import com.chrono.worker.config.WorkerValidationProperties;
import com.chrono.worker.services.JobProcessingService;
import com.chrono.worker.services.retry.RetryHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class JobEventConsumer {

    private final ObjectMapper objectMapper;
    private final JobProcessingService jobProcessingService;
    private final RetryHandler retryHandler;
    private final double simulatedFailureRate;
    private final Random random;

    public JobEventConsumer(ObjectMapper objectMapper,
                            JobProcessingService jobProcessingService,
                            RetryHandler retryHandler,
                            WorkerValidationProperties workerValidationProperties) {
        this.objectMapper = objectMapper;
        this.jobProcessingService = jobProcessingService;
        this.retryHandler = retryHandler;
        this.simulatedFailureRate = workerValidationProperties.getSimulatedFailureRate();
        Long randomSeed = workerValidationProperties.getRandomSeed();
        this.random = randomSeed == null ? new Random() : new Random(randomSeed);
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
        JobEventModel jobEvent;
        try {
            jobEvent = objectMapper.readValue(message, JobEventModel.class);
        } catch (Exception e) {
            log.error("Failed to deserialize message from topic:{} with partition:{} and offset:{} and key:{}",
                    topic,
                    partition, offset, key, e);
            throw new IllegalStateException("Deserialization failed: " + e.getMessage(), e);
        }

        try {
            validateJob(jobEvent);
            log.info("Consuming message - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    topic, partition, offset, key);
            jobProcessingService.processJobEvent(jobEvent);
            jobEvent.setStatus(JobStatus.COMPLETED);
            acknowledgment.acknowledge();
            log.info("Successfully processed job: {} from topic: {}",
                    jobEvent.getJobId(), topic);
        } catch (Exception processingError) {
            log.error("Job {} failed: {}", jobEvent.getJobId(), processingError.getMessage());
            jobEvent.setStatus(JobStatus.FAILED);
            try {
                retryHandler.handleFailure(jobEvent, processingError);
                acknowledgment.acknowledge();
                log.info("Job {} handed off to retry/DLQ and acknowledged (attempt {}/{})",
                        jobEvent.getJobId(), jobEvent.getRetryCount(), jobEvent.getMaxRetries());
            } catch (Exception ex) {
                log.error("Retry/DLQ handoff failed for the job {}. Record will not be not be acknowledged.",
                        jobEvent.getJobId(), ex);
                throw new IllegalStateException(
                        "Failed to handle retry/DLQ for job " + jobEvent.getJobId() + ": " + ex.getMessage(), ex);
            }
        }
    }

    private void validateJob(JobEventModel jobEvent) {
        if (jobEvent == null) {
            throw new JobExecutionException(false, "Invalid job event: null payload");
        }

        if (jobEvent.getJobId() == null || jobEvent.getJobId().isBlank()) {
            throw new JobExecutionException(false, "Invalid job event: missing jobId");
        }

        if (jobEvent.getRetryCount() < 0 || jobEvent.getMaxRetries() < 0) {
            throw new JobExecutionException(false,
                    "Invalid retry config for job " + jobEvent.getJobId());
        }

        if (jobEvent.getRetryCount() > jobEvent.getMaxRetries()) {
            throw new JobExecutionException(false,
                    "Retry count exceeded max retries for job " + jobEvent.getJobId());
        }

        try {
            JobPayloadValidator.validateCompatibility(jobEvent.getJobType(), jobEvent.getPayload());
        } catch (IllegalArgumentException ex) {
            throw new JobExecutionException(false,
                    "Invalid payload for job " + jobEvent.getJobId() + ": " + ex.getMessage());
        }

        if (simulatedFailureRate < 0.0 || simulatedFailureRate > 1.0) {
            throw new JobExecutionException(false,
                    "Invalid worker.validation.simulated-failure-rate: " + simulatedFailureRate);
        }

        double roll = random.nextDouble();

        log.info("Validation - JobId: {}, SimulatedFailureRate: {}, Roll: {}",
                jobEvent.getJobId(), simulatedFailureRate, roll);

        if (roll < simulatedFailureRate) {
            throw new JobExecutionException(true,
                    "Simulated transient failure for job " + jobEvent.getJobId());
        }
    }
}
