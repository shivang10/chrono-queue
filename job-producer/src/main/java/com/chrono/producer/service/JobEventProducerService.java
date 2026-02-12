package com.chrono.producer.service;

import com.chrono.common.constants.KafkaTopics;
import com.chrono.common.model.JobEventModel;
import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import com.chrono.producer.mapper.JobProducerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class JobEventProducerService {

    private static final Logger logger = Logger.getLogger(JobEventProducerService.class.getName());
    private final JobProducerMapper jobProducerMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public JobEventProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, JobProducerMapper jobProducerMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.jobProducerMapper = jobProducerMapper;
        logger.info("JobEventProducerService initialized");
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
            logger.info("Produced Job Event: " + jobEventModel.toString());

            JobEventResponseDTO jobEventResponseDTO = jobProducerMapper.toJobEventResponse(jobEventModel);
            return jobEventResponseDTO;
        } catch (Exception e) {
            logger.severe("Error producing job event: " + e.getMessage());
            return new JobEventResponseDTO("Failed to produce job event");
        }
    }

    private void sendToKafka(String topicName, String key, String value) {
        if (topicName == null || key == null || value == null) {
            logger.severe("Cannot send to Kafka: topicName, key, or value is null");
            return;
        }
        kafkaTemplate.send(topicName, key, value)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        logger.log(Level.SEVERE, "Error sending job event to Kafka", exception);
                    } else {
                        logger.info(String.format("Job event sent successfully - Topic: %s, Partition: %d, Offset: %d",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()));
                    }
                });
    }

}