package com.chrono.worker.services.dlq;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.enums.JobStatus;
import com.chrono.common.model.JobEventModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DlqProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(JobEventModel job, Exception ex) {
        try {
            job.setStatus(JobStatus.FAILED);
            String payload = objectMapper.writeValueAsString(job);

            kafkaTemplate.send(KafkaTopics.JOB_DLQ, job.getJobId(), payload)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to send job {} to DLQ", job.getJobId(), exception);
                        } else {
                            log.info("Job {} sent to DLQ - Topic: {}, Partition: {}, Offset: {}, Reason: {}",
                                    job.getJobId(),
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset(),
                                    ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error serializing job {} for DLQ", job.getJobId(), e);
        }
    }
}
