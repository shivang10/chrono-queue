package com.chrono.retry.scheduler;

import com.chrono.common.model.JobEventModel;
import com.chrono.retry.config.RetryPollerProperties;
import com.chrono.retry.producer.JobRequeueProducer;
import com.chrono.retry.repository.RetryQueueRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RetryPoller {

    private final RetryQueueRepository retryQueueRepository;
    private final JobRequeueProducer jobRequeueProducer;
    private final ObjectMapper objectMapper;
    private final RetryPollerProperties retryPollerProperties;

    private final AtomicBoolean polling = new AtomicBoolean(false);

    private final Timer pollTimer;
    private final Counter pollCounter;
    private final Counter dispatchedCounter;
    private final Counter deserializationFailureCounter;
    private final Counter pollSkippedCounter;

    public RetryPoller(RetryQueueRepository retryQueueRepository,
            JobRequeueProducer jobRequeueProducer,
            ObjectMapper objectMapper,
            RetryPollerProperties retryPollerProperties,
            MeterRegistry meterRegistry) {
        this.retryQueueRepository = retryQueueRepository;
        this.jobRequeueProducer = jobRequeueProducer;
        this.objectMapper = objectMapper;
        this.retryPollerProperties = retryPollerProperties;

        this.pollTimer = Timer.builder("retry.poller.poll.duration")
                .description("Time taken for a single poll cycle")
                .register(meterRegistry);
        this.pollCounter = Counter.builder("retry.poller.polls.total")
                .description("Total number of poll cycles executed")
                .register(meterRegistry);
        this.dispatchedCounter = Counter.builder("retry.poller.dispatched.total")
                .description("Total number of jobs dispatched for retry")
                .register(meterRegistry);
        this.deserializationFailureCounter = Counter.builder("retry.poller.deserialization.failures.total")
                .description("Total number of jobs that failed deserialization")
                .register(meterRegistry);
        this.pollSkippedCounter = Counter.builder("retry.poller.skipped.total")
                .description("Total number of polls skipped due to overlap")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${retry.poller.fixed-delay:1000}")
    public void pollAndDispatch() {
        if (!polling.compareAndSet(false, true)) {
            log.warn("Previous poll cycle still in progress, skipping");
            pollSkippedCounter.increment();
            return;
        }

        try {
            pollTimer.record(this::doPoll);
        } finally {
            polling.set(false);
        }
    }

    private void doPoll() {
        pollCounter.increment();

        List<String> dueJobs = retryQueueRepository.fetchDueJobs(
                retryPollerProperties.getBatchSize(),
                System.currentTimeMillis());

        if (dueJobs.isEmpty()) {
            log.debug("No due jobs to retry at this poll");
            return;
        }

        log.info("Polling retry queue - {} due jobs found", dueJobs.size());

        for (String jobJson : dueJobs) {
            try {
                JobEventModel job = objectMapper.readValue(jobJson, JobEventModel.class);
                // Use the original JSON from Redis directly - avoids redundant re-serialization
                jobRequeueProducer.requeue(job, jobJson);
                dispatchedCounter.increment();
            } catch (JsonProcessingException e) {
                deserializationFailureCounter.increment();
                String preview = jobJson.length() > 200 ? jobJson.substring(0, 200) + "…" : jobJson;
                log.error("Failed to deserialize retry job payload, skipping - payload(truncated): {}, error: {}",
                        preview, e.getMessage(), e);
            }
        }
    }
}
