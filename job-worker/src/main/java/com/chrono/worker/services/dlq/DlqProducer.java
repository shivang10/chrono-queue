package com.chrono.worker.services.dlq;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.enums.JobStatus;
import com.chrono.common.model.JobEventModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class DlqProducer {

    private static final Logger logger = Logger.getLogger(DlqProducer.class.getName());
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
                            logger.log(Level.SEVERE, String.format(
                                    "Failed to send job %s to DLQ", job.getJobId()), exception);
                        } else {
                            logger.info(String.format(
                                    "Job %s sent to DLQ - Topic: %s, Partition: %d, Offset: %d, Reason: %s",
                                    job.getJobId(),
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset(),
                                    ex.getMessage()));
                        }
                    });
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format(
                    "Error serializing job %s for DLQ", job.getJobId()), e);
        }
    }
}
