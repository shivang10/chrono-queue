package com.chrono.worker.services.dlq;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.enums.JobStatus;
import com.chrono.common.exceptions.JobPayloadSerializationException;
import com.chrono.common.exceptions.JobPublishException;
import com.chrono.common.model.JobEventModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class DlqProducer {
    private static final long SEND_TIMEOUT_SECONDS = 5L;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DlqProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void send(JobEventModel job, Exception ex) {
        job.setStatus(JobStatus.FAILED);
        log.warn("Sending job to DLQ - jobId: {}, jobType: {}, reason: {}",
                job.getJobId(), job.getJobType(), ex.getMessage());

        try {
            String payload = objectMapper.writeValueAsString(job);
            SendResult<String, String> result = kafkaTemplate.send(KafkaTopics.JOB_DLQ, job.getJobId(), payload)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            RecordMetadata metadata = result.getRecordMetadata();
            log.info("Job sent to DLQ - jobId: {}, topic: {}, partition: {}, offset: {}, reason: {}",
                    job.getJobId(),
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    ex.getMessage());
        } catch (JsonProcessingException serializationException) {
            throw new JobPayloadSerializationException(
                    "Failed to serialize job " + job.getJobId() + " for DLQ publishing",
                    serializationException);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new JobPublishException(
                    "Interrupted while publishing job " + job.getJobId() + " to DLQ",
                    interruptedException);
        } catch (ExecutionException | TimeoutException publishException) {
            throw new JobPublishException(
                    "Failed to publish job " + job.getJobId() + " to the DLQ",
                    publishException);
        }
    }
}
