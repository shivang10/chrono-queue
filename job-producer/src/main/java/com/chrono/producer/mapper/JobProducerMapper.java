package com.chrono.producer.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.chrono.common.enums.JobStatus;
import com.chrono.common.model.JobEventModel;
import com.chrono.producer.dto.jobEventDto.JobEventRequestDTO;
import com.chrono.producer.dto.jobEventDto.JobEventResponseDTO;

@Component
public class JobProducerMapper {
    public JobProducerMapper() {
    }

    public JobEventModel toJobEvent(JobEventRequestDTO jobEventRequestDTO) {
        JobEventModel jobEventModel = new JobEventModel();
        jobEventModel.setJobType(jobEventRequestDTO.getJobType());
        jobEventModel.setPayload(jobEventRequestDTO.getPayload());
        jobEventModel.setCreatedAt(LocalDateTime.now());
        jobEventModel.setRetryCount(0);
        jobEventModel.setMaxRetries(3);
        jobEventModel.setStatus(JobStatus.PENDING);
        return jobEventModel;
    }


    public JobEventResponseDTO toJobEventResponse(JobEventModel jobEventModel) {
        JobEventResponseDTO jobEventResponseDTO = new JobEventResponseDTO();
        jobEventResponseDTO.setMessage("Job created successfully");
        return jobEventResponseDTO;
    }
}
