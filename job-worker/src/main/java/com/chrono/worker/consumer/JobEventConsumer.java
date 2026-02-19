package com.chrono.worker.consumer;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.enums.JobStatus;
import com.chrono.common.exceptions.JobExecutionException;
import com.chrono.common.model.JobEventModel;
import com.chrono.worker.services.JobProcessingService;
import com.chrono.worker.services.retry.RetryHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.beans.factory.annotation.Value;
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
            @Value("${worker.validation.simulatedFailureRate:0.5}") double simulatedFailureRate,
            @Value("${worker.validation.randomSeed:#{null}}") Long randomSeed) {
        this.objectMapper = objectMapper;
        this.jobProcessingService = jobProcessingService;
        this.retryHandler = retryHandler;
        this.simulatedFailureRate = simulatedFailureRate;
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
        try {
            JobEventModel jobEvent = objectMapper.readValue(message, JobEventModel.class);
            try {
                validateJob(jobEvent);
                log.info("Consuming message - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                        topic, partition, offset, key);
                jobProcessingService.processJobEvent(jobEvent);
                jobEvent.setStatus(JobStatus.COMPLETED);
                log.info("Successfully processed job: {} from topic: {}",
                        jobEvent.getJobId(), topic);
            } catch (Exception e) {
                log.error("Job {} failed: {}", jobEvent.getJobId(), e.getMessage());
                jobEvent.setStatus(JobStatus.FAILED);
                retryHandler.handleFailure(jobEvent, e);
                log.info("Job {} scheduled for retry (attempt {}/{})",
                        jobEvent.getJobId(), jobEvent.getRetryCount(), jobEvent.getMaxRetries());
            }
        } catch (Exception e) {
            log.error("Fatal error processing message: {}", e.getMessage(), e);
        } finally {
            acknowledgment.acknowledge();
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

        if (simulatedFailureRate < 0.0 || simulatedFailureRate > 1.0) {
            throw new JobExecutionException(false,
                    "Invalid worker.validation.simulatedFailureRate: " + simulatedFailureRate);
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
