package com.chrono.producer.service;

import com.chrono.common.model.JobEventModel;
import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import com.chrono.producer.mapper.JobProducerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class JobEventProducerService {

    private static final Logger logger = Logger.getLogger(JobEventProducerService.class.getName());
    private static JobProducerMapper jobProducerMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Value("${kafka.topic.job-events:job-events}")
    private String topicName;

    public JobEventProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, JobProducerMapper jobProducerMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        JobEventProducerService.jobProducerMapper = jobProducerMapper;
        logger.info("JobEventProducerService initialized");
    }

    public JobEventResponseDTO produceJobEvent(JobEventRequestDTO jobEventRequestDTO) {
        try {
            if (jobEventRequestDTO == null || jobEventRequestDTO.getJobType() == null) {
                throw new IllegalArgumentException("Job event request or job type cannot be null");
            }
            JobEventModel jobEventModel = jobProducerMapper.toJobEvent(jobEventRequestDTO);
            String jobEventJson = objectMapper.writeValueAsString(jobEventModel);
            sendToKafka(jobEventModel.getJobType().toString(), jobEventJson);
            logger.info("Produced Job Event: " + jobEventModel.toString());
            if (jobEventRequestDTO == null || jobEventRequestDTO.getJobType() == null) {
                throw new IllegalArgumentException("Job event request or job type cannot be null");
            }
            JobEventResponseDTO jobEventResponseDTO = jobProducerMapper.toJobEventResponse(jobEventModel);
            return jobEventResponseDTO;
        } catch (Exception e) {
            logger.severe("Error producing job event: " + e.getMessage());
            return new JobEventResponseDTO("Failed to produce job event");
        }
    }

    private void sendToKafka(String key, String value) {
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