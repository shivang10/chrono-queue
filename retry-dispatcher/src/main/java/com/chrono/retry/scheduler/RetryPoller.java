package com.chrono.retry.scheduler;

import com.chrono.common.model.JobEventModel;
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

    public RetryPoller(RetryQueueRepository retryQueueRepository,
                       JobRequeueProducer jobRequeueProducer,
                       ObjectMapper objectMapper) {
        this.retryQueueRepository = retryQueueRepository;
        this.jobRequeueProducer = jobRequeueProducer;
        this.objectMapper = objectMapper;
        log.info("RetryPoller initialized successfully.");
    }

    @Scheduled(fixedDelayString = "${retry.poller.fixedDelay:5000}")
    public void pollAndDispatch() {
        List<String> dueJobs = retryQueueRepository.fetchDueJobs(10, System.currentTimeMillis());

        if (dueJobs.isEmpty()) {
            return;
        }

        log.info("Fetched {} due jobs for retry", dueJobs.size());

        for (String jobString : dueJobs) {
            try {
                JobEventModel job = objectMapper.readValue(jobString, JobEventModel.class);
                String jobEventJson = objectMapper.writeValueAsString(job);
                jobRequeueProducer.requeue(job, jobEventJson);
            } catch (JsonProcessingException e) {
                log.error("Error processing job JSON: {}", jobString, e);
            }
        }
    }
}
