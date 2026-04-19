package com.chrono.retry.scheduler;

import com.chrono.common.model.JobEventModel;
import com.chrono.retry.config.RetryPollerProperties;
import com.chrono.retry.producer.JobRequeueProducer;
import com.chrono.retry.repository.RetryQueueRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RetryPoller {

    private final RetryQueueRepository retryQueueRepository;
    private final JobRequeueProducer jobRequeueProducer;
    private final ObjectMapper objectMapper;
    private final RetryPollerProperties retryPollerProperties;

    public RetryPoller(RetryQueueRepository retryQueueRepository,
            JobRequeueProducer jobRequeueProducer,
            ObjectMapper objectMapper,
            RetryPollerProperties retryPollerProperties) {
        this.retryQueueRepository = retryQueueRepository;
        this.jobRequeueProducer = jobRequeueProducer;
        this.objectMapper = objectMapper;
        this.retryPollerProperties = retryPollerProperties;
    }

    @Scheduled(fixedDelayString = "#{@retryPollerProperties.getFixedDelay()}")
    public void pollAndDispatch() {
        List<String> dueJobs = retryQueueRepository.fetchDueJobs(
                retryPollerProperties.getBatchSize(),
                System.currentTimeMillis());

        if (dueJobs.isEmpty()) {
            log.debug("No due jobs to retry at this poll");
            return;
        }

        log.info("Polling retry queue - {} due jobs found", dueJobs.size());

        for (String jobString : dueJobs) {
            try {
                JobEventModel job = objectMapper.readValue(jobString, JobEventModel.class);
                String jobEventJson = objectMapper.writeValueAsString(job);
                jobRequeueProducer.requeue(job, jobEventJson);
            } catch (JsonProcessingException e) {
                String preview = jobString.length() > 200 ? jobString.substring(0, 200) + "…" : jobString;
                log.error("Failed to deserialize retry job payload, skipping - payload(truncated): {}, error: {}",
                        preview, e.getMessage(), e);
            }
        }
    }
}
