package com.chrono.producer.service;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.chrono.common.model.JobEventModel;
import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JobEventProducerService {

    private static final Logger logger = Logger.getLogger(JobEventProducerService.class.getName());
    
    @Value("${kafka.topic.job-events:job-events}")
    private String topicName;

    private final KafkaProducer<String, String> kafkaProducer;
    private final ObjectMapper objectMapper;

    public JobEventProducerService(KafkaProducer<String, String> kafkaProducer, ObjectMapper objectMapper) {
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = objectMapper;
        logger.info("JobEventProducerService initialized");
    }

    public JobEventResponseDTO produceJobEvent(JobEventRequestDTO jobEventRequestDTO) {
        try{
            if (jobEventRequestDTO == null || jobEventRequestDTO.getJobType() == null) {
                throw new IllegalArgumentException("Job event request or job type cannot be null");
            }
            JobEventModel jobEventModel = createJobEventModel(jobEventRequestDTO);

            String jobEventJson = objectMapper.writeValueAsString(jobEventModel);
            sendToKafka(jobEventModel.getJobType().toString(), jobEventJson);
            // ProducerRecord<String, String> record = new ProducerRecord<String, String>(topicName, jobEventModel.getJobType().toString(), jobEventJson);
            // kafkaProducer.send(record, (metadata, exception) -> {
            //     if (exception != null) {
            //         logger.severe("Error sending message to Kafka: " + exception.getMessage());
            //     } else {
            //         logger.info("Message sent to topic " + metadata.topic() + " partition " + metadata.partition() + " offset " + metadata.offset());
            //     }
            // });
            logger.info("Produced Job Event: " + jobEventModel.toString());


            if (jobEventRequestDTO == null || jobEventRequestDTO.getJobType() == null) {
                throw new IllegalArgumentException("Job event request or job type cannot be null");
            }
            JobEventResponseDTO jobEventResponseDTO = new JobEventResponseDTO();
            jobEventResponseDTO.setMessage("Job event produced successfully");
            return jobEventResponseDTO;
        } catch (Exception e) {
            logger.severe("Error producing job event: " + e.getMessage());
            return new JobEventResponseDTO("Failed to produce job event");
        }
    }

    private void sendToKafka(String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, value);
        
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                logger.log(Level.SEVERE, "Error sending job event to Kafka", exception);
            } else {
                logger.info(String.format("Job event sent successfully - Topic: %s, Partition: %d, Offset: %d", 
                    metadata.topic(), metadata.partition(), metadata.offset()));
            }
        });
    }

    private JobEventModel createJobEventModel(JobEventRequestDTO dto) {
        JobEventModel jobEventModel = new JobEventModel();
        jobEventModel.setJobType(dto.getJobType());
        jobEventModel.setPayload(dto.getPayload());
        jobEventModel.setCreatedAt(LocalDateTime.now());
        return jobEventModel;
    }
    
}