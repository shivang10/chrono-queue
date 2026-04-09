package com.chrono.producer.service;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.exceptions.InvalidJobRequestException;
import com.chrono.common.exceptions.JobPayloadSerializationException;
import com.chrono.common.exceptions.JobPublishException;
import com.chrono.common.model.JobEventModel;
import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import com.chrono.producer.mapper.JobProducerMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class JobEventProducerService {
    private static final long PUBLISH_TIMEOUT_SECONDS = 5L;

    private final JobProducerMapper jobProducerMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public JobEventProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                   JobProducerMapper jobProducerMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.jobProducerMapper = jobProducerMapper;
        log.info("JobEventProducerService initialized");
    }

    public JobEventResponseDTO produceJobEvent(JobEventRequestDTO jobEventRequestDTO) {
        validateRequest(jobEventRequestDTO);

        JobEventModel jobEventModel = jobProducerMapper.toJobEvent(jobEventRequestDTO);
        String jobEventJson = serialize(jobEventModel);
        String topicName = resolveTopic(jobEventModel);

        sendToKafka(topicName, jobEventModel.getJobId(), jobEventJson);
        log.info("Produced Job Event: {}", jobEventModel);
        return jobProducerMapper.toJobEventResponse(jobEventModel);
    }

    private void sendToKafka(String topicName, String key, String value) {
        if (topicName == null || key == null || value == null) {
            throw new InvalidJobRequestException("Topic, key, and payload are required to publish a job event");
        }

        try {
            SendResult<String, String> result = kafkaTemplate.send(topicName, key, value)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            RecordMetadata metadata = result.getRecordMetadata();
            log.info("Job event sent successfully - Topic: {}, Partition: {}, Offset: {}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new JobPublishException("Interrupted while publishing job event", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new JobPublishException("Failed to publish job event to Kafka", ex);
        }
    }

    private void validateRequest(JobEventRequestDTO jobEventRequestDTO) {
        if (jobEventRequestDTO == null) {
            throw new InvalidJobRequestException("Job event request cannot be null");
        }

        if (jobEventRequestDTO.getJobType() == null) {
            throw new InvalidJobRequestException("Job type cannot be null");
        }

        if (jobEventRequestDTO.getPayload() == null || jobEventRequestDTO.getPayload().isEmpty()) {
            throw new InvalidJobRequestException("Payload cannot be empty");
        }
    }

    private String serialize(JobEventModel jobEventModel) {
        try {
            return objectMapper.writeValueAsString(jobEventModel);
        } catch (JsonProcessingException ex) {
            throw new JobPayloadSerializationException(
                    "Failed to serialize job " + jobEventModel.getJobId() + " for publishing",
                    ex);
        }
    }

    private String resolveTopic(JobEventModel jobEventModel) {
        try {
            return KafkaTopics.getTopicForJobType(jobEventModel.getJobType());
        } catch (IllegalArgumentException ex) {
            throw new InvalidJobRequestException(ex.getMessage(), ex);
        }
    }

}