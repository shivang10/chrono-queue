package com.chrono.worker.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.chrono.common.model.JobEventModel;
import com.chrono.worker.services.JobProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JobEventConsumer {
    
    private final ObjectMapper objectMapper;
    private final JobProcessingService jobProcessingService;

    public JobEventConsumer(ObjectMapper objectMapper, JobProcessingService jobProcessingService) {
        this.objectMapper = objectMapper;
        this.jobProcessingService = jobProcessingService;
    }

    @KafkaListener(topics = "${kafka.topic.job-events}", groupId = "job-worker-group")
    public void consume(String message) throws Exception {
        JobEventModel jobEvent = objectMapper.readValue(message, JobEventModel.class);
        jobProcessingService.processJobEvent(jobEvent);
    }
}
