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
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class JobEventConsumer {

    private final ObjectMapper objectMapper;
    private final JobProcessingService jobProcessingService;
    private final RetryHandler retryHandler;
    private final MeterRegistry meterRegistry;
    private final double simulatedFailureRate;
    private final Random seededRandom;

    public JobEventConsumer(ObjectMapper objectMapper,
            JobProcessingService jobProcessingService,
            RetryHandler retryHandler,
            WorkerValidationProperties workerValidationProperties,
            MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.jobProcessingService = jobProcessingService;
        this.retryHandler = retryHandler;
        this.meterRegistry = meterRegistry;
        this.simulatedFailureRate = workerValidationProperties.getSimulatedFailureRate();
        Long randomSeed = workerValidationProperties.getRandomSeed();
        this.seededRandom = randomSeed == null ? null : new Random(randomSeed);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_JOBS, concurrency = "${worker.kafka.concurrency.payment-processing-jobs:6}")
    public void consumePayment(
            @Payload List<String> message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            Acknowledgment acknowledgment) {

        consume(message, topics, partitions, offsets, keys, acknowledgment);
    }

    @KafkaListener(topics = KafkaTopics.EMAIL_JOBS, concurrency = "${worker.kafka.concurrency.email-jobs:3}")
    public void consumeEmail(
            @Payload List<String> message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            Acknowledgment acknowledgment) {

        consume(message, topics, partitions, offsets, keys, acknowledgment);
    }

    @KafkaListener(topics = KafkaTopics.WEBHOOK_JOBS, concurrency = "${worker.kafka.concurrency.webhook-jobs:3}")
    public void consumeWebhook(
            @Payload List<String> message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            Acknowledgment acknowledgment) {

        consume(message, topics, partitions, offsets, keys, acknowledgment);
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLATION_JOBS, concurrency = "${worker.kafka.concurrency.order-cancellation-jobs:3}")
    public void consumeCancellation(
            @Payload List<String> message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            Acknowledgment acknowledgment) {

        consume(message, topics, partitions, offsets, keys, acknowledgment);
    }

    private void consume(
            List<String> messages,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            Acknowledgment acknowledgment) {
        for (int i = 0; i < messages.size(); i++) {

            String message = messages.get(i);
            String topic = topics.get(i);
            int partition = partitions.get(i);
            long offset = offsets.get(i);
            String key = keys.get(i);
            JobEventModel jobEvent;
            try {
                jobEvent = objectMapper.readValue(message, JobEventModel.class);
            } catch (Exception e) {
                log.error("Failed to deserialize message - topic: {}, partition: {}, offset: {}, key: {}",
                        topic, partition, offset, key, e);
                meterRegistry.counter("chrono.jobs.consumed",
                        "job_type", "unknown",
                        "result", "malformed").increment();
                continue;
            }
            try {

                log.info("Consuming message - topic: {}, partition: {}, offset: {}, jobId: {}",
                        topic, partition, offset, key);
                validateJob(jobEvent);
                jobProcessingService.processJobEvent(jobEvent);
                jobEvent.setStatus(JobStatus.COMPLETED);
                meterRegistry.counter("chrono.jobs.consumed",
                        "job_type", jobEvent.getJobType().name(),
                        "result", "success").increment();
                log.info("Job processed successfully - jobId: {}, jobType: {}, topic: {}, partition: {}, offset: {}",
                        jobEvent.getJobId(), jobEvent.getJobType(), topic, partition, offset);
            } catch (Exception processingError) {
                log.warn("Job processing failed - jobId: {}, jobType: {}, attempt: {}/{}, reason: {}",
                        jobEvent.getJobId(), jobEvent.getJobType(),
                        jobEvent.getRetryCount() + 1, jobEvent.getMaxRetries() + 1,
                        processingError.getMessage(), processingError);
                jobEvent.setStatus(JobStatus.FAILED);
                try {
                    retryHandler.handleFailure(jobEvent, processingError);
                    log.info("Job failure handled - jobId: {}, attempt: {}/{}, acknowledged",
                            jobEvent.getJobId(), jobEvent.getRetryCount(), jobEvent.getMaxRetries());
                } catch (Exception ex) {
                    log.error("Retry/DLQ handoff failed for jobId: {} — record will NOT be acknowledged",
                            jobEvent.getJobId(), ex);
                    throw new IllegalStateException(
                            "Failed to handle retry/DLQ for job " + jobEvent.getJobId() + ": " + ex.getMessage(), ex);
                }
            }
        }
        acknowledgment.acknowledge();
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

        double roll = nextFailureRoll();

        log.debug("Simulated failure check - jobId: {}, failureRate: {}, roll: {}",
                jobEvent.getJobId(), simulatedFailureRate, roll);

        if (roll < simulatedFailureRate) {
            throw new JobExecutionException(true,
                    "Simulated transient failure for job " + jobEvent.getJobId());
        }
    }

    private double nextFailureRoll() {
        if (seededRandom == null) {
            return ThreadLocalRandom.current().nextDouble();
        }

        synchronized (seededRandom) {
            return seededRandom.nextDouble();
        }
    }
}
