package com.chrono.producer.service;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import com.chrono.producer.mapper.JobProducerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobEventProducerService {

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
        try {

            if (jobEventRequestDTO == null || jobEventRequestDTO.getJobType() == null) {
                throw new IllegalArgumentException("Job event request or job type cannot be null");
            }

            JobEventModel jobEventModel = jobProducerMapper.toJobEvent(jobEventRequestDTO);
            String jobEventJson = objectMapper.writeValueAsString(jobEventModel);
            String topicName = KafkaTopics.getTopicForJobType(jobEventModel.getJobType());

            sendToKafka(topicName, jobEventModel.getJobId().toString(), jobEventJson);
            log.info("Produced Job Event: {}", jobEventModel);

            JobEventResponseDTO jobEventResponseDTO = jobProducerMapper.toJobEventResponse(jobEventModel);
            return jobEventResponseDTO;
        } catch (Exception e) {
            log.error("Error producing job event: {}", e.getMessage(), e);
            return new JobEventResponseDTO("Failed to produce job event");
        }
    }

    private void sendToKafka(String topicName, String key, String value) {
        if (topicName == null || key == null || value == null) {
            log.error("Cannot send to Kafka: topicName, key, or value is null");
            return;
        }
        kafkaTemplate.send(topicName, key, value)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Error sending job event to Kafka", exception);
                    } else {
                        log.info("Job event sent successfully - Topic: {}, Partition: {}, Offset: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

}